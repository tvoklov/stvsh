package v1.folder

import cats.effect.IO
import com.volk.stvsh.db.Aliases.ID
import com.volk.stvsh.db.objects.folder.Folder
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.Sheet
import com.volk.stvsh.db.objects.folder.Schema.FolderSchema
import play.api.libs.json.{Format, Json}
import play.api.mvc.{AbstractController, ControllerComponents, _}
import play.api.mvc.Results.EmptyContent

import java.util.UUID
import javax.inject.Inject

class FolderController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {

  // stinky head cause stinky play likes to fill in it's own stinky headers
  // will return CONTENT_LENGTH with a little underscore because read the above
  // for now is here only to be a "does this folder even exist?" call
  // todo rewrite to a proper head, after figuring out how
  def head(id: String): Action[AnyContent] = Action.async {
    val io = for {
      maybeFolder <- id.getFolder.perform
      res <- maybeFolder.fold(IO.pure(NotFound("no folder with given id")))(
        _ =>
          Sheet.count(id).perform.map {
            x => Ok(EmptyContent()).withHeaders(CONTENT_LENGTH + "_" -> x.toString)
          }
      )
    } yield res

    io.unsafeToFuture()
  }

  def getSheets(id: String)(offset: Option[Long], limit: Option[Long], sortBy: Option[String]): Action[AnyContent] = Action.async {
    val io =
      for {
        maybeFolder <- id.getFolder.perform
        res <- maybeFolder.fold(IO.pure(NotFound("no folder with given id"))) {
          f =>
            f
              .getSheets(offset, limit)
              .perform
              .map(_.map(_.toJson))
              .map(Json.toJson(_))
              .map(Ok(_))
        }
      } yield res

    io.unsafeToFuture()
  }

  def get(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      val io = for {
        folder <- Folder
          .get(id)
          .perform
      } yield folder.map(_.toJson).fold(NotFound("no folder with given id found"))(Ok(_))

      io.unsafeToFuture()
  }

  def post: Action[AnyContent] = Action.async {
    implicit request =>
      val io = request.body.asJson
        .map(_.as[PreSaveFolder])
        .fold(IO.pure(BadRequest("incorrect json structure"))) {
          newFolder =>
            val folder = newFolder.toFolder
            for { _ <- folder.save.perform } yield Ok(folder.toJson)
        }

      io.unsafeToFuture()
  }

}

object PreSaveFolder {
  implicit val preSaveFolderJson: Format[PreSaveFolder] = Json.format
}

case class PreSaveFolder(
                          id: Option[ID],
                          name: String,
                          ownerId: ID,
                          schema: FolderSchema
                        ) {
  def toFolder: Folder = Folder(id.getOrElse(UUID.randomUUID().toString), name, ownerId, schema)
}
