package v1.sheet

import cats.effect.IO
import com.volk.stvsh.db.objects.Sheet
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.folder.Schema.Key
import com.volk.stvsh.db.objects.SheetField.SheetField
import play.api.libs.json.{ Format, Json }
import play.api.mvc.{ AbstractController, Action, AnyContent, ControllerComponents }

import java.util.UUID
import javax.inject.Inject

class SheetController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {
  def put(id: String) = Action.async {
    implicit request =>
      val io = request.body.asJson
        .flatMap(_.asOpt[PreSaveSheet])
        .fold(IO.pure(BadRequest("bad json value for a sheet"))) {
          sheet =>
            sheet.folderId.fold(
              for {
                maybeExistingSheet <-
                  sheet.id
                    .getOrElse(id)
                    .getSheet
                    .perform
                res <- maybeExistingSheet match {
                  case None => IO.pure(NotFound("no sheet for given id found"))
                  case Some(s) =>
                    s.copy(values = sheet.values)
                      .save
                      .perform
                      .map(
                        _ => Ok(s.toJson)
                      )
                }
              } yield res
            ) {
              fid =>
                val savedSheet = sheet.toSheet(fid)
                savedSheet.save.perform.map(
                  _ => Ok(savedSheet.toJson)
                )
            }
        }

      io.unsafeToFuture()
  }

  def post(folderId: String): Action[AnyContent] = Action.async {
    implicit request =>
      val io = request.body.asJson
        .map(_.as[PreSaveSheet])
        .map(_.toSheet(folderId))
        .fold(IO.pure(BadRequest("bad json value for a sheet"))) {
          sheet =>
            folderId.getFolder.perform
              .flatMap(_.fold(IO.pure(BadRequest("folder with given id not found"))) {
                folder =>
                  if (!Sheet.validate(folder)(sheet))
                    IO.pure(BadRequest("sheet violates folder structure"))
                  else
                    sheet
                      .copy(id = UUID.randomUUID().toString, folderId = folderId)
                      .save
                      .perform
                      .map(
                        _ => Ok(sheet.toJson)
                      )
              })
        }

      io.unsafeToFuture()
  }

  def get(id: String): Action[AnyContent] = Action.async {
    val io = for {
      sheet <- Sheet.get(id).perform
    } yield sheet.map(_.toJson).fold(NotFound("no sheet with given id"))(Ok(_))

    io.unsafeToFuture()
  }

}

case class PreSaveSheet(values: Map[Key, SheetField], folderId: Option[String], id: Option[String]) {
  def toSheet(folderId: String): Sheet =
    Sheet(UUID.randomUUID().toString, folderId, values)

}

object PreSaveSheet {
  implicit def format: Format[PreSaveSheet] = Json.format
}
