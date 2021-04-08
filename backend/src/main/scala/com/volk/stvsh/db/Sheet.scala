package com.volk.stvsh.db

import cats.free.Free
import com.volk.stvsh.db.Aliases._
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.Fields.{ SheetField, _ }
import com.volk.stvsh.db.Schema.{ Key, ValueType }
import com.volk.stvsh.extensions.SqlUtils.SqlFixer
import doobie._
import doobie.free.connection
import doobie.implicits._
import doobie.util.fragment
import doobie.util.fragment.Fragment
import play.api.libs.json._

import java.util.UUID
import scala.util.Try

case class Sheet(
    id: ID,
    folderId: ID,
    values: Map[Key, SheetField]
)

object Sheet {
  private[db] val pgTable = "sheet"

  private object fields {
    val id = "id"
    val values = "value"
    val folderId = "folder_id"
  }

  def apply(folder: Folder, values: Map[Key, SheetField]): Sheet =
    Sheet(UUID.randomUUID().toString, folder.id, values)

  def validate(folder: Folder): Sheet => Boolean =
    _.values.forall { case (key, v) =>
      folder.schema.get(key).contains(v.valueType)
    }

  private def toSheet: ((ID, ID, Map[Key, SheetField])) => Sheet = { case (id, folderId, values) =>
    Sheet(id, folderId, values)
  }

  def get(id: ID): Free[connection.ConnectionOp, Option[Sheet]] = {
    val sql =
      s"""
         |select ${fields.id}, ${fields.values}, ${fields.folderId} from $pgTable
         |where ${fields.id} = '$id'
         |""".stripMargin

    Fragment
      .const(sql)
      .query[(ID, String, ID)]
      .option
      .map(_.map { case (id, valuesJson, folderId) =>
        (id, folderId, Json.parse(valuesJson).as[Map[Key, SheetField]])
      }.map(toSheet))
  }

  def insert: Sheet => Fragment = { case Sheet(id, folderId, values) =>
    val sql =
      s"""
         |insert into $pgTable
         |(${fields.id}, ${fields.folderId}, ${fields.values})
         |values('$id', '$folderId', '${Json.toJson(values).toString().fixForSql}')
         |""".stripMargin

    Fragment.const(sql)
  }

  def update: Sheet => Fragment = { case Sheet(id, _, values) =>
    val sql =
      s"""
         |update $pgTable
         |set ${fields.values} = ${Json.toJson(values).toString().fixForSql}
         |where ${fields.id} = '$id'
         |""".stripMargin

    Fragment.const(sql)
  }

  def delete: Sheet => Fragment = { case Sheet(id, _, _) =>
    sql"delete from " ++ Fragment.const(pgTable) ++ sql" where " ++ Fragment.const(fields.id) ++ sql" = $id"
  }

  def save(sheet: Sheet): ConnectionIO[Int] = {
    val check =
      sql"select ${fields.id} from " ++ Fragment.const(pgTable) ++
        sql" where " ++ Fragment.const(fields.id) ++ sql" = ${sheet.id}"

    check
      .query[String]
      .option
      .flatMap(_.fold(insert(sheet))(_ => update(sheet)).update.run)
  }

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

    Fragment
      .const(sql)
      .query[(ID, String, ID)]
      .to[List]
      .map {
        _.map { case (id, valuesJson, folderId) =>
          (id, folderId, Json.parse(valuesJson).as[Map[Key, SheetField]])
        }
      }
      .map(_.map(toSheet))
  }

  def parseSheet(folderId: ID, valuesJson: String): Free[connection.ConnectionOp, Option[Map[String, SheetField]]] =
    folderId.getFolder.map(_.map { case Folder(_, _, _, schema) =>
      Json
        .parse(valuesJson)
        .as[Map[String, String]]
        .flatMap { case key -> value =>
          schema
            .get(key)
            .flatMap {
              case ValueType.text                => Some(Text(value))
              case ValueType.image               => Some(Image(value))
              case ValueType.wholeNumber         => value.toIntOption.map(WholeNumber(_))
              case ValueType.floatingPointNumber => value.toFloatOption.map(FloatingPointNumber)
              case ValueType.tags                => Try(Json.parse(value).as[List[String]]).toOption.map(Tags)
            }
            .map(key -> _)
        }
    })
}

object Fields {
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

  implicit val seetFieldFormat: Format[SheetField] = new Format[SheetField] {
    override def reads(json: JsValue): JsResult[SheetField] = {
      val type_ = json \ "type"
      val value = json \ "value"

      type_.toOption
        .zip(value.toOption)
        .map { case (t, v) =>
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
