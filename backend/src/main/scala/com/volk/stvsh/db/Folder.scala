package com.volk.stvsh.db

import cats.free.Free
import com.volk.stvsh.db.Access.AccessType.AccessType
import com.volk.stvsh.db.Aliases._
import com.volk.stvsh.db.Schema.FolderSchema
import com.volk.stvsh.extensions.CatsExtensions._
import com.volk.stvsh.extensions.DoobieExtensions._
import com.volk.stvsh.extensions.SqlUtils._
import doobie.{ ConnectionIO, Fragment }
import doobie.free.connection.ConnectionOp
import doobie.implicits._
import play.api.libs.json.{ Format, Json }

import java.util.UUID

case class Folder(
    id: ID,
    name: String,
    ownerId: ID,
    schema: FolderSchema,
)

object Folder {

  private val pgTable = "folder"

  private object fields {
    val id = "id"
    val name = "name"
    val ownerId = "owner_id"
    val schema = "schema"
  }

  def apply(name: String, owner: User, schema: FolderSchema): Folder =
    Folder(UUID.randomUUID().toString, name, owner.id, schema)

  def get(id: String): doobie.ConnectionIO[Option[Folder]] = {
    val sql =
      sql"select ${fields.id}, ${fields.name}, ${fields.ownerId}, ${fields.schema} from " ++ Fragment.const(pgTable) ++
        sql" where " ++ Fragment.const(fields.id) ++ sql" = $id"

    sql
      .query[(ID, String, ID, String)]
      .option
      .map(_.map { case (id, name, userId, schemaJson) =>
        Folder(id, name, userId, Json.parse(schemaJson).as[FolderSchema])
      })
  }

  def save: Folder => ConnectionIO[Int] = { case Folder(id, name, ownerId, schema) =>
    val sql =
      s"""
         |insert into $pgTable
         |(${fields.id}, ${fields.name}, ${fields.ownerId}, ${fields.schema})
         |values('$id', '${name.fixForSql}', '$ownerId', '${Json.toJson(schema).toString().fixForSql}')
         |""".stripMargin

    Fragment.const(sql).update.run
  }

  def findBy(
      ownerId: Option[String] = None,
      userId: Option[String] = None,
  ): ConnectionIO[List[Folder]] = {
    val filters =
      List(
        ownerId.map(fields.ownerId + " = '" + _ + "'"),
        userId.map(s"${fields.id} in (select ${Access.fields.folderId} from ${Access.pgTable} where ${Access.fields.userId} = '" + _ + "'")
      ).flatten.mkString("where ", " and ", "")

    val sql =
      sql"select ${fields.id}, ${fields.name}, ${fields.ownerId}, ${fields.schema} from " ++ Fragment.const(pgTable) ++ sql" " ++
        Fragment.const(filters)

    sql
      .query[(ID, String, ID, String)]
      .to[List]
      .map(_.map { case (id, name, userId, schemaJson) =>
        Folder(id, name, userId, Json.parse(schemaJson).as[FolderSchema])
      })
  }

  def allowAccess(
      user: User,
      allow: Boolean = true,
      accessTypes: List[AccessType] = Nil,
  ): Folder => ConnectionIO[Int] = { case Folder(id, _, _, _) =>
    val insert = Access.insert(id, user.id)

    Access
      .getFor(id, user.id)
      .flatMap {
        case Nil if allow =>
          accessTypes
            .map(insert)
            .combine
            .update
            .run
        case list =>
          val (o, n) = accessTypes.partition(list.contains)
          val sql =
            if (allow) n.map(insert).combine
            else Access.delete(id, user.id, o)

          sql.update.run
      }
  }

  def getUsers: Folder => ConnectionIO[List[(User, List[AccessType])]] = { case Folder(id, _, ownerId, _) =>
    val owner: Free[ConnectionOp, Option[(User, List[AccessType])]] =
      User
        .get(ownerId)
        .map(_.map(_ -> (Access.AccessType.full :: Nil)))

    val users = Access.getUsers(id)

    owner.flatMap(o => users.map(x => o.toList ++ x))
  }

  implicit val folderJson: Format[Folder] = Json.format
}

object Access {
  private[db] val pgTable = "folder_access"

  private[db] object fields {
    val folderId = "folder_id"
    val userId = "user_id"
    val accessType = "access_type"
  }

  object AccessType {
    type AccessType = String

    val full = "FULL"
    val read = "READ"
    val write = "WRITE"
  }

  def insert(folderId: ID, userId: ID): AccessType => Fragment =
    accessType => {
      val sql =
        s"""
           |insert into $pgTable
           |(${fields.folderId}, ${fields.userId}, ${fields.accessType})
           |values('$folderId', '$userId', '$accessType')
           |""".stripMargin
      Fragment.const(sql)
    }

  def delete(folderId: ID, userId: ID, accessTypes: List[AccessType] = Nil): Fragment = {
    val sql =
      s"""
         |delete from $pgTable
         |where ${fields.folderId} = '$folderId' and ${fields.userId} = '$userId'
         |${accessTypes.mkString(s"and ${fields.accessType} in ('", "', '", "')")}
         |""".stripMargin

    Fragment.const(sql)
  }

  def getFor(folderId: ID, userId: ID): doobie.ConnectionIO[List[AccessType]] = {
    val sql =
      s"""
         |select ${fields.accessType} from $pgTable
         |where ${fields.folderId} = '$folderId' and ${fields.userId} = '$userId'
         |""".stripMargin

    Fragment.const(sql).query[AccessType].to[List]
  }

  def getUsers(id: ID): ConnectionIO[List[(User, List[AccessType])]] = {
    val sql =
      s"""
         |select ${fields.userId}, ${fields.accessType} from $pgTable
         |where folder_id = $id
         |""".stripMargin

    Fragment
      .const(sql)
      .query[(ID, String)]
      .to[List]
      .flatMap(
        _.groupMap(_._1)(_._2)
          .map { case (id, accessTypes) => User.get(id).map(_ -> accessTypes) }
          .sequence
          .map(_.flatMap { case (ost, atps) => ost.map(_ -> atps) })
      )
  }

}
