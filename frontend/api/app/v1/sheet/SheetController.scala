package v1.sheet

import cats.effect.IO
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.Sheet
import com.volk.stvsh.db.objects.SheetField.SheetField
import com.volk.stvsh.db.objects.folder.Schema.Key
import com.volk.stvsh.extensions.PlayJson._
import play.api.libs.json.{ Format, Json }
import play.api.mvc._

import java.util.UUID
import javax.inject.Inject

class SheetController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {

  def get(id: String): Action[AnyContent] = Action.async {
    id.getSheet.perform
      .map {
        case None        => BadRequest("no sheet with given id")
        case Some(value) => Ok(value.toJson)
      }
      .unsafeToFuture()
  }

  def update(id: String): Action[AnyContent] = Action.async {
    request =>
      val io = request.body.asJson.flatMap(_.asOpt[PreSaveSheet]) match {
        case None => IO.pure(BadRequest("bad json value for a sheet"))
        case Some(sheet) =>
          for {
            sheet <- sheet.folderId match {
              case Some(fId) => IO.pure(Right(sheet.toSheet(id = id, fId)))
              case None =>
                for {
                  maybeSheet <- sheet.id.getOrElse(id).getSheet.perform
                  res <- maybeSheet match {
                    case None        => IO.pure(Left("no sheet for given id found"))
                    case Some(value) => IO.pure(Right(value.copy(values = sheet.values)))
                  }
                } yield res
            }
            x <- sheet match {
              case Left(value)  => IO.pure(Left(value))
              case Right(value) => saveSheetIO(value)
            }
          } yield lrToResult[Sheet](_.toJson)(x)
      }

      io.unsafeToFuture()
  }

  def create(folderId: String): Action[AnyContent] = Action.async {
    request =>
      val io = request.body.asJson.flatMap(_.asOpt[PreSaveSheet]) match {
        case None        => IO.pure(BadRequest("bad json value for a sheet"))
        case Some(sheet) => saveSheetIO(sheet.toSheet(folderId = folderId)).map(lrToResult(_.toJson))
      }

      io.unsafeToFuture()
  }

  def saveSheetIO(sheet: Sheet): IO[Either[String, Sheet]] =
    for {
      folder <- sheet.folderId.getFolder.perform
      response <-
        folder match {
          case None => IO.pure(Left("folder with given id not found"))
          case Some(folder) =>
            val sheetConformsToFolder = Sheet.validate(folder)(sheet)
            if (sheetConformsToFolder)
              sheet.save.perform.map(
                _ => Right(sheet)
              )
            else IO.pure(Left("sheet violates folder structure"))
        }
    } yield response

  def lrToResult[T](func: T => String): Either[String, T] => Result = {
    case Left(value)  => BadRequest(value)
    case Right(value) => Ok(func(value))
  }

}

case class PreSaveSheet(id: Option[String], folderId: Option[String], values: Map[Key, SheetField]) {
  def toSheet(id: String = UUID.randomUUID().toString, folderId: String): Sheet =
    Sheet(this.id.getOrElse(id), this.folderId.getOrElse(folderId), values)

}

object PreSaveSheet {
  implicit def format: Format[PreSaveSheet] = Json.format
}
