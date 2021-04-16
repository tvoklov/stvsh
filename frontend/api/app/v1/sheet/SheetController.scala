package v1.sheet

import cats.effect.IO
import com.volk.stvsh.db.objects.Sheet
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.folder.Schema.Key
import com.volk.stvsh.db.objects.SheetField.SheetField
import com.volk.stvsh.extensions.PlayJson._
import play.api.libs.json._
import play.api.mvc._

import java.util.UUID
import javax.inject.Inject

class SheetController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {

  def get(id: String): Action[AnyContent] = Action.async {
    val io = for {
      sheet <- Sheet.get(id).perform
    } yield sheet.map(_.toJson).fold(NotFound("no sheet with given id"))(Ok(_))

    io.unsafeToFuture()
  }

  def post(folderId: String): Action[AnyContent] = Action.async {
    implicit request =>
      val io = request.body.asJson
        .flatMap(_.asOpt[PreSaveSheet])
        .map(_.toSheet(folderId = folderId))
        .fold(IO.pure(BadRequest("bad json value"))) {
          sheet =>
            for {
              f <- sheet.folderId.getFolder.perform
              s <- f.fold(IO.pure(BadRequest("folder with given id not found")))(
                folder =>
                  if (!Sheet.validate(folder)(sheet))
                    IO.pure(BadRequest("sheet violates folder structure"))
                  else
                    sheet
                      .copy(id = UUID.randomUUID().toString, folderId = folderId)
                      .save
                      .perform
                      .map(_.toJson)
                      .map(Ok(_))
              )
            } yield s
        }

      io.unsafeToFuture()
  }

  def put(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      val io = request.body.asJson
        .flatMap(_.asOpt[PreSaveSheet])
        .fold(IO.pure(BadRequest("bad json value"))) {
          sheet =>
            sheet.folderId.fold(
              IO.pure(BadRequest("bad json value - there can not be a sheet without a folder"))
            ) {
              _ =>
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
            }
        }

      io.unsafeToFuture()
  }

}

case class PreSaveSheet(id: Option[String], folderId: Option[String], values: Map[Key, SheetField]) {
  def toSheet(id: String = UUID.randomUUID().toString, folderId: String): Sheet =
    Sheet(this.id.getOrElse(id), this.folderId.getOrElse(folderId), values)
}

object PreSaveSheet {
  implicit def format: Format[PreSaveSheet] = Json.format
}
