package v1.sheet

import cats.implicits._
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.{ Sheet, UserSession }
import com.volk.stvsh.db.objects.SheetField.SheetField
import com.volk.stvsh.db.objects.folder.Schema.Key
import com.volk.stvsh.extensions.Play.{ ActionBuilderOps, EitherResultable }
import com.volk.stvsh.extensions.PlayJson._
import com.volk.stvsh.HeaderNames
import com.volk.stvsh.db.objects.folder.FolderAccess
import com.volk.stvsh.db.objects.folder.FolderAccess.CanRead
import doobie.ConnectionIO
import doobie.implicits._
import play.api.libs.json.{ Format, Json }
import play.api.mvc._

import java.util.UUID
import javax.inject.Inject

class SheetController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {

  def withSession[T](func: UserSession => ConnectionIO[Result]): Request[AnyContent] => ConnectionIO[Result] =
    _.headers.get(HeaderNames.session) match {
      case Some(sid) =>
        UserSession.get(sid) match {
          case Some(us) => func(us)
          case None     => BadRequest("session does not exist").pure[ConnectionIO]
        }
      case None => BadRequest("no session").pure[ConnectionIO]
    }

  def withSessionCheckAction(check: UserSession => ConnectionIO[Boolean], checkError: String = "Can not do this with current session")(
      func: Request[AnyContent] => ConnectionIO[Result]
  ): Action[AnyContent] =
    Action.asyncF(withSessionCheck(check, checkError)(func)(_).perform)

  def withSessionCheck(check: UserSession => ConnectionIO[Boolean], checkError: String = "Can not do this with current session")(
      func: Request[AnyContent] => ConnectionIO[Result]
  ): Request[AnyContent] => ConnectionIO[Result] =
    request =>
      withSession {
        us =>
          for {
            f   <- check(us)
            res <- if (f) func(request) else Forbidden(checkError).pure[ConnectionIO]
          } yield res
      }(request)

  def get: String => Action[AnyContent] = id =>
    Action.asyncF { request =>
      val cio = for {
        maybeSheet <- id.getSheet
        res <-
          maybeSheet match {
            case None        => NotFound("").pure[ConnectionIO]
            case Some(sheet) =>
              withSessionCheck(us => FolderAccess.hasAccessType(sheet.folderId)(us.userId)(CanRead), "No Access to Folder with Sheet")(
                _ => Ok(sheet.toJson).pure[ConnectionIO]
                )(request)
          }
      } yield res

      cio.perform
    }

//  def get(id: String): Action[AnyContent] = Action.asyncF {
//    request =>
//      id.getSheet.map {
//        case None        => NotFound("No sheet with given id")
//        case Some(value) => Ok(value.toJson)
//      }.perform
//  }

  def update(id: String): Action[AnyContent] = Action.asyncF {
    request =>
      val cio = request.body.asJson.flatMap(_.asOpt[PreSaveSheet]) match {
        case None        => BadRequest("Bad json").pure[ConnectionIO]
        case Some(sheet) => Sheet.updateValues(id)(sheet.values).map(_.toResult(_.toJson))
      }

      cio.perform
  }

  def create(folderId: String): Action[AnyContent] = Action.asyncF {
    request =>
      val cio = request.body.asJson.flatMap(_.asOpt[PreSaveSheet]) match {
        case None        => BadRequest("bad json value for a sheet").pure[ConnectionIO]
        case Some(sheet) => Sheet.checkAndSave(sheet.toSheet(folderId = folderId)).map(_.toResult(_.toJson))
      }

      cio.perform
  }

}

case class PreSaveSheet(id: Option[String], folderId: Option[String], values: Map[Key, SheetField]) {
  def toSheet(id: String = UUID.randomUUID().toString, folderId: String): Sheet =
    Sheet(this.id.getOrElse(id), this.folderId.getOrElse(folderId), values)
}

object PreSaveSheet {
  implicit def format: Format[PreSaveSheet] = Json.format
}
