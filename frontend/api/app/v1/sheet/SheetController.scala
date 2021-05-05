package v1.sheet

import cats.implicits._
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.Sheet
import com.volk.stvsh.db.objects.SheetField.SheetField
import com.volk.stvsh.db.objects.folder.FolderAccess
import com.volk.stvsh.db.objects.folder.FolderAccess.{ CanReadSheets, CanWriteSheets }
import com.volk.stvsh.db.objects.folder.Schema.Key
import com.volk.stvsh.extensions.Play.{ ActionBuilderOps, EitherResultable }
import com.volk.stvsh.extensions.PlayJson._
import com.volk.stvsh.v1.SessionManagement.withSessionCheck
import doobie.ConnectionIO
import doobie.implicits._
import play.api.libs.json.{ Format, Json }
import play.api.mvc._

import java.util.UUID
import javax.inject.Inject

class SheetController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {

  def get: String => Action[AnyContent] = id =>
    Action.asyncF {
      request =>
        val cio = for {
          maybeSheet <- id.getSheet
          res <-
            maybeSheet match {
              case None => NotFound("").pure[ConnectionIO]
              case Some(sheet) =>
                withSessionCheck(us => FolderAccess.hasAccessType(sheet.folderId)(us.userId)(CanReadSheets).pure[ConnectionIO], "no access to folder")(
                  _ => Ok(sheet.toJson).pure[ConnectionIO]
                )(request)
            }
        } yield res

        cio.perform
    }

  def update: String => Action[AnyContent] = id =>
    Action.asyncF {
      request =>
        val cio = for {
          maybeSheet <- id.getSheet
          res <-
            maybeSheet match {
              case None => NotFound("").pure[ConnectionIO]
              case Some(existingSheet) =>
                withSessionCheck(us => FolderAccess.hasAccessType(existingSheet.folderId)(us.userId)(CanWriteSheets).pure[ConnectionIO], "no access to folder") {
                  _ =>
                    request.body.asJson.flatMap(_.asOpt[PreSaveSheet]) match {
                      case None          => BadRequest("Bad json").pure[ConnectionIO]
                      case Some(preSave) => for { up <- Sheet.updateValues(existingSheet)(preSave.values) } yield up.toResult(_.toJson)
                    }
                }(request)
            }
        } yield res

        cio.perform
    }

  def create: String => Action[AnyContent] = folderId =>
    Action.asyncF {
      request =>
        withSessionCheck(
          us => FolderAccess.hasAccessType(folderId)(us.userId)(CanWriteSheets).pure[ConnectionIO]
        ) {
          _ =>
            request.body.asJson.flatMap(_.asOpt[PreSaveSheet]) match {
              case None        => BadRequest("bad json value for a sheet").pure[ConnectionIO]
              case Some(sheet) => Sheet.checkAndSave(sheet.toSheet(folderId = folderId)).map(_.toResult(_.toJson))
            }
        }(request).perform
    }

}

case class PreSaveSheet(id: Option[String], folderId: Option[String], values: Map[Key, SheetField]) {
  def toSheet(id: String = UUID.randomUUID().toString, folderId: String): Sheet =
    Sheet(this.id.getOrElse(id), this.folderId.getOrElse(folderId), values)
}

object PreSaveSheet {
  implicit def format: Format[PreSaveSheet] = Json.format
}
