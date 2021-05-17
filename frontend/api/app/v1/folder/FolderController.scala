package v1.folder

import cats.implicits._
import com.volk.stvsh.db.Aliases.ID
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.Sheet
import com.volk.stvsh.db.objects.folder.Folder
import com.volk.stvsh.db.objects.folder.FolderAccess.{CanReadSheets, CanWriteSheets}
import com.volk.stvsh.db.objects.folder.Schema.FolderSchema
import com.volk.stvsh.extensions.Play._
import com.volk.stvsh.extensions.PlayJson._
import com.volk.stvsh.v1.SessionManagement._
import doobie.ConnectionIO
import play.api.libs.json.{Format, Json}
import play.api.mvc.Results.EmptyContent
import play.api.mvc.{AbstractController, ControllerComponents, _}

import java.util.UUID
import javax.inject.Inject

class FolderController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {

  def get: String => Action[AnyContent] = id =>
    Action.asyncF {
      ifHasFolderAccess(CanReadSheets)(id)(
        _ =>
          Folder
            .get(id)
            .map {
              case None         => NotFound("no folder with given id")
              case Some(folder) => Ok(folder.toJson)
            }
      ) andThen (_.perform)
    }

  def create: Action[AnyContent] = Action.asyncF {
    request =>
      withSession(
        us =>
          request.body.asJson.map(_.as[PreSaveFolder]) match {
            case None => BadRequest("bad json value").pure[ConnectionIO]
            case Some(f) =>
              val folder = f.toFolder(us.userId)
              for { _ <- folder.save } yield Ok(folder.toJson)
          }
      )(request).perform
  }

  def getSheets(id: String)(offset: Option[Long], limit: Option[Long], sortBy: Option[String]): Action[AnyContent] =
    Action.asyncF {
      ifHasFolderAccess(CanReadSheets)(id)(
        _ =>
          for {
            maybeFolder <- id.getFolder
            res <- maybeFolder match {
              case None    => NotFound("no folder with given id").pure[ConnectionIO]
              case Some(f) => for { s <- f.getSheets(offset, limit) } yield Ok(s.toJson)
            }
          } yield res
      ) andThen (_.perform)
    }

  // !!! WARNING unsafe operation, does not account for session !!!
  // stinky head cause stinky play likes to fill in it's own stinky headers
  // will return CONTENT_LENGTH with a little underscore because read the above
  // for now is here only to be a "does this folder even exist?" call
  // todo rewrite to a proper head, after figuring out how
  def head(id: String): Action[AnyContent] = Action.asyncF {
    val cio = for {
      folderExists <- Folder.exists(id)
      res <-
        if (folderExists)
          Sheet
            .count(id)
            .map(
              x =>
                Ok(EmptyContent())
                  .withHeaders(CONTENT_LENGTH + "_" -> x.toString)
            )
        else NotFound("no folder with given id").pure[ConnectionIO]
    } yield res

    cio.perform
  }

}

object PreSaveFolder {
  implicit val preSaveFolderJson: Format[PreSaveFolder] = Json.format
}

case class PreSaveFolder(id: Option[ID], name: String, ownerId: Option[ID], schema: FolderSchema) {
  def toFolder(owner: ID): Folder = Folder(id.getOrElse(UUID.randomUUID().toString), name, ownerId.getOrElse(owner), schema)
}
