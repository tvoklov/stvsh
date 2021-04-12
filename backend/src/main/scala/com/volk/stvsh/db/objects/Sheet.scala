package com.volk.stvsh.db.objects

import com.volk.stvsh.db.Aliases._
import com.volk.stvsh.db.objects.SheetField.{ SheetField, _ }
import com.volk.stvsh.db.objects.folder.Folder
import com.volk.stvsh.db.objects.folder.Schema.{ Key, ValueType }
import com.volk.stvsh.extensions.SqlUtils.SqlFixer
import play.api.libs.json._

import java.util.UUID

case class Sheet(
    id: ID,
    folderId: ID,
    values: Map[Key, SheetField]
)

object Sheet {
  import doobie._
  import doobie.implicits._
  import doobie.util.fragment.Fragment

  private[db] val pgTable = "sheet"

  private object fields {
    val id       = "id"
    val values   = "value"
    val folderId = "folder_id"
  }

  def apply(folder: Folder, values: Map[Key, SheetField]): Sheet =
    Sheet(UUID.randomUUID().toString, folder.id, values)

  private def toSheet: ((ID, String, ID)) => Sheet = {
    case (id, valuesJson, folderId) =>
      Sheet(id, folderId, Json.parse(valuesJson).as[Map[Key, SheetField]])
  }

  def validate(folder: Folder): Sheet => Boolean =
    _.values.forall {
      case (key, v) =>
        folder.schema.get(key).contains(v.valueType)
    }

  def get: ID => ConnectionIO[Option[Sheet]] =
    CRUD
      .select(_)
      .query[(ID, String, ID)]
      .option
      .map(_.map(toSheet))

  def save(sheet: Sheet): ConnectionIO[Int] = {
    val check =
      sql"select ${fields.id} from " ++ Fragment.const(pgTable) ++
        sql" where " ++ Fragment.const(fields.id) ++ sql" = ${sheet.id}"

    check
      .query[String]
      .option
      .flatMap(
        _.fold(CRUD.insert(sheet))(
          _ => CRUD.update(sheet)
        ).update.run
      )
  }

  def delete: Sheet => ConnectionIO[Int] = CRUD.delete(_).update.run

  def findBy(folderId: Option[ID], offset: Option[Long], limit: Option[Long]): ConnectionIO[List[Sheet]] = {
    val filters =
      List(
        folderId.map(fields.folderId + " = '" + _ + "'"),
      ).flatten.mkString("where ", " and ", "")

    val paging =
      List(
        offset.map("offset" + _),
        limit.map("limit" + _)
      ).flatten
        .mkString(" ")

    val sql =
      s"""
         |select ${fields.id}, ${fields.values}, ${fields.folderId} from $pgTable
         |$filters
         |$paging
         |""".stripMargin

    asFragment(sql)
      .query[(ID, String, ID)]
      .to[List]
      .map(_.map(toSheet))
  }

  private object CRUD {
    def select: ID => Fragment = asFragment compose {
      id =>
        s"""
           |select ${fields.id}, ${fields.values}, ${fields.folderId} from $pgTable
           |where ${fields.id} = '$id'
           |""".stripMargin
    }

    def insert: Sheet => Fragment = asFragment compose {
      case Sheet(id, folderId, values) =>
        s"""
           |insert into $pgTable
           |(${fields.id}, ${fields.folderId}, ${fields.values})
           |values('$id', '$folderId', '${Json.toJson(values).toString().fixForSql}')
           |""".stripMargin
    }

    def update: Sheet => Fragment = asFragment compose {
      case Sheet(id, _, values) =>
        s"""
           |update $pgTable
           |set ${fields.values} = '${Json.toJson(values).toString().fixForSql}'
           |where ${fields.id} = '$id'
           |""".stripMargin
    }

    def delete: Sheet => Fragment = asFragment compose {
      case Sheet(id, _, _) =>
        s"""
           |delete from $pgTable
           |where ${fields.id} = '$id'
           |""".stripMargin
    }
  }

  implicit class SheetJson(sheet: Sheet) {
    def toJson: String = Json.toJson(sheet).toString()
  }

  implicit val sheetJson: Format[Sheet] = Json.format
}

object SheetField {
  sealed trait SheetField {
    def valueType: ValueType
  }
  case class Text(value: String) extends SheetField {
    override def valueType: ValueType = ValueType.text
  }
  case class WholeNumber(value: Long) extends SheetField {
    override def valueType: ValueType = ValueType.wholeNumber
  }
  case class FloatingPointNumber(value: Float) extends SheetField {
    override def valueType: ValueType = ValueType.floatingPointNumber
  }
  case class Image(value: String) extends SheetField {
    override def valueType: ValueType = ValueType.image
  }
  case class Tags(value: List[String]) extends SheetField {
    override def valueType: ValueType = ValueType.tags
  }

  implicit val sheetFieldFormat: Format[SheetField] = new Format[SheetField] {
    override def reads(json: JsValue): JsResult[SheetField] = {
      val type_ = json \ "type"
      val value = json \ "value"

      type_.toOption
        .zip(value.toOption)
        .map {
          case (t, v) =>
            t.as[String] match {
              case ValueType.text                => Text(v.as[String])
              case ValueType.wholeNumber         => WholeNumber(v.as[Long])
              case ValueType.floatingPointNumber => FloatingPointNumber(v.as[Float])
              case ValueType.image               => Image(v.as[String])
              case ValueType.tags                => Tags(v.as[List[String]])
            }
        }
        .fold[JsResult[SheetField]](JsError("something went wrong"))(JsSuccess[SheetField](_))
    }

    override def writes(o: SheetField): JsValue = JsObject(
      Seq(
        "type" -> JsString(o.valueType),
        "value" -> (o match {
          case Text(value)                => JsString(value)
          case WholeNumber(value)         => JsNumber(value)
          case FloatingPointNumber(value) => JsNumber(value)
          case Image(value)               => JsString(value)
          case Tags(value)                => Json.toJson(value)
        })
      )
    )
  }

}
