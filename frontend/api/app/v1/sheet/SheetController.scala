package v1.sheet

import cats.implicits._
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.Sheet
import com.volk.stvsh.db.objects.SheetField.SheetField
import com.volk.stvsh.db.objects.folder.Schema.Key
import com.volk.stvsh.extensions.Play.PlayIO
import com.volk.stvsh.extensions.PlayJson._
import doobie.ConnectionIO
import doobie.implicits._
import play.api.libs.json.{Format, Json}
import play.api.mvc._

import java.util.UUID
import javax.inject.Inject

class SheetController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {

  def get(id: String): Action[AnyContent] = Action.async {
    id.getSheet
      .map {
        case None        => NotFound("No sheet with given id")
        case Some(value) => Ok(value.toJson)
      }
      .perform
      .toResultFuture
  }

  def update(id: String): Action[AnyContent] = Action.async {
    request =>
      val io = request.body.asJson.flatMap(_.asOpt[PreSaveSheet]) match {
        case None        => BadRequest("Bad json").pure[ConnectionIO]
        case Some(sheet) => Sheet.updateValues(id)(sheet.values).map(lrToResult(_.toJson))
      }

      io.perform.toResultFuture
  }

  def create(folderId: String): Action[AnyContent] = Action.async {
    request =>
      val io = request.body.asJson.flatMap(_.asOpt[PreSaveSheet]) match {
        case None        => BadRequest("bad json value for a sheet").pure[ConnectionIO]
        case Some(sheet) => Sheet.checkAndSave(sheet.toSheet(folderId = folderId)).map(lrToResult(_.toJson))
      }

      io.perform.toResultFuture
  }

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
