package v1.sheet

import com.volk.stvsh.db.objects.folder.Schema.{ Key, ValueType }
import com.volk.stvsh.db.objects.SheetField.SheetField
import com.volk.stvsh.db.Aliases.SheetValues
import com.volk.stvsh.db.objects.{ Sheet, SheetField }
import com.volk.stvsh.db.objects.folder.Folder
import play.api.libs.json.{ Format, Json }
import v1.sheet.PreSaveSheet._

import java.util.UUID

object PreSaveSheet {
  type PreSaveSheetField = String

  private def toSheetField(valueType: ValueType)(field: PreSaveSheetField): Either[String, SheetField] =
    valueType match {
      case ValueType.text =>
        Right(SheetField.Text(field))
      case ValueType.image =>
        Right(SheetField.Image(field))
      case ValueType.tags =>
        val tags = field.split(";").toList //todo decide on separator for tags
        Right(SheetField.Tags(tags))
      case ValueType.wholeNumber =>
        field.toLongOption
             .toRight(s"can not parse $field as int")
             .map(SheetField.WholeNumber)
      case ValueType.floatingPointNumber =>
        field.toFloatOption
             .toRight(s"can not parse $field as float")
             .map(SheetField.FloatingPointNumber)
      case _ => Left(s"unknown type $valueType")
    }

  implicit def format: Format[PreSaveSheet] = Json.format
}

case class PreSaveSheet(id: Option[String], folderId: Option[String], values: Map[Key, PreSaveSheetField]) {
  def toSheet(id: String = UUID.randomUUID().toString, folder: Folder): Either[String, Sheet] =
    for {
      properValues <- {
        val schema = folder.schema
        values.foldLeft[Either[String, SheetValues]](Right(Map.empty[Key, SheetField])) {
          case (acc, (key, fieldValue)) =>
            acc.flatMap(
              prev =>
                schema.get(key) match {
                  case Some(fieldType) => toSheetField(fieldType)(fieldValue).map(key -> _).map(prev + _)
                  case None            => Left(s"schema does not contain key $key")
                }
              )
        }
      }
    } yield Sheet(this.id.getOrElse(id), this.folderId.getOrElse(folder.id), properValues)
}
