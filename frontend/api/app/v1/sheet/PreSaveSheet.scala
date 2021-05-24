package v1.sheet

import com.volk.stvsh.db.objects.folder.Schema.{ Key, ValueType }
import com.volk.stvsh.db.objects.SheetField.SheetField
import com.volk.stvsh.db.Aliases.SheetValues
import com.volk.stvsh.db.objects.folder.Folder
import com.volk.stvsh.db.objects.{ Sheet, SheetField }
import play.api.libs.json.{ Format, Json }

import java.util.UUID

case class PreSaveSheet(id: Option[String], folderId: Option[String], values: Map[Key, PreSaveSheetField]) {
  def toSheet(id: String = UUID.randomUUID().toString, folder: Folder): Either[String, Sheet] =
    for {
      properValues <- {
        val schema = folder.schema
        values.foldLeft[Either[String, SheetValues]](Right(Map.empty[Key, SheetField])) {
          case (acc, (key, pssf)) =>
            acc.flatMap(
              prev =>
                schema.get(key) match {
                  case Some(value) => pssf.toSheetField(value).map(key -> _).map(prev + _)
                  case None        => Left(s"schema does not contain key $key")
                }
              )
        }
      }
    } yield Sheet(this.id.getOrElse(id), this.folderId.getOrElse(folder.id), properValues)
}

case class PreSaveSheetField(value: String) {
  def toSheetField(valueType: ValueType): Either[String, SheetField] =
    valueType match {
      case ValueType.text =>
        Right(SheetField.Text(value))
      case ValueType.image =>
        Right(SheetField.Image(value))
      case ValueType.tags =>
        val tags = value.split(";").toList //todo decide on separator for tags
        Right(SheetField.Tags(tags))
      case ValueType.wholeNumber =>
        value.toLongOption
             .toRight(s"can not parse $value as int")
             .map(SheetField.WholeNumber)
      case ValueType.floatingPointNumber =>
        value.toFloatOption
             .toRight(s"can not parse $value as float")
             .map(SheetField.FloatingPointNumber)
      case _ => Left(s"unknown type $valueType")
    }
}

object PreSaveSheet {
  implicit def format: Format[PreSaveSheet] = Json.format
  implicit def preSaveSheetFieldFormat: Format[PreSaveSheetField] = Json.format
}
