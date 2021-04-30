package v1.sheet

import cats.implicits._
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.Sheet
import com.volk.stvsh.db.objects.SheetField.SheetField
import com.volk.stvsh.db.objects.folder.Schema.Key
import com.volk.stvsh.extensions.Play.{ ActionBuilderOps, EitherResultable, PlayIO }
import com.volk.stvsh.extensions.PlayJson._
import doobie.ConnectionIO
import doobie.implicits._
import play.api.http.Writeable
import play.api.libs.json.{ Format, Json }
import play.api.mvc._

import java.util.UUID
import javax.inject.Inject

class SheetController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {

  def get(id: String): Action[AnyContent] = Action.asyncF {
    id.getSheet.map {
      case None        => NotFound("No sheet with given id")
      case Some(value) => Ok(value.toJson)
    }.perform
  }

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
