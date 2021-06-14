package com.volk.stvsh.db.objects.folder

import com.volk.stvsh.db.Aliases._
import com.volk.stvsh.db.objects._
import com.volk.stvsh.db.objects.folder.FolderAccess.AccessType
import com.volk.stvsh.db.objects.folder.Schema.FolderSchema
import com.volk.stvsh.extensions.Sql._
import doobie._
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
    val id      = "id"
    val name    = "name"
    val ownerId = "owner_id"
    val schema  = "schema"
  }

  def apply(name: String, owner: User, schema: FolderSchema): Folder =
    Folder(UUID.randomUUID().toString, name, owner.id, schema)

  private def toFolder: ((ID, String, ID, String)) => Folder = {
    case (id, name, userId, schemaJson) =>
      Folder(id, name, userId, Json.parse(schemaJson).as[FolderSchema])
  }

  def exists: ID => ConnectionIO[Boolean]     = CRUD.exists
  def get: ID => ConnectionIO[Option[Folder]] = CRUD.select
  def delete: ID => ConnectionIO[Int]         = CRUD.delete

  def save: Folder => ConnectionIO[Int] = {
    case f @ Folder(id, _, _, _) =>
      exists(id)
        .flatMap(
          if (_) CRUD.update(f)
          else CRUD.insert(f)
        )
  }

  def update(id: ID, name: Option[String], ownerId: Option[ID]): ConnectionIO[Int] =
    CRUD.updateParts(id, name, ownerId)

  def findBy(
      ownerId: Option[String] = None,
      userId: Option[String] = None,
  ): ConnectionIO[List[Folder]] = {
    val ownerUserFilter =
      List(
        ownerId.map(fields.ownerId + " = '" + _ + "'"),
        userId.map(
          s"${fields.id} in (select ${FolderAccess.fields.folderId} from ${FolderAccess.pgTable} where ${FolderAccess.fields.userId} = '" + _ + "')"
        )
      ).flatten.mkString("where (", ") or (", ")")

    val sql =
      s"""
         |select ${fields.id}, ${fields.name}, ${fields.ownerId}, ${fields.schema} from $pgTable
         |$ownerUserFilter
         |""".stripMargin

    asFragment(sql)
      .query[(ID, String, ID, String)]
      .to[List]
      .map(_.map(toFolder))
  }

  /** @return users with any kind of access to the folder, including the owner. */
  def getAllUsersWithAccess: Folder => ConnectionIO[List[(User, List[AccessType])]] = {
    case Folder(id, _, ownerId, _) =>
      val owner: ConnectionIO[Option[(User, List[AccessType])]] =
        User
          .get(ownerId)
          .map(_.map(_ -> (FolderAccess.CanEditFolder :: FolderAccess.CanReadSheets :: FolderAccess.CanWriteSheets :: Nil)))

      val users = FolderAccess.getUsersWithAccess(id)

      owner.flatMap(
        o =>
          users.map(
            x => o.toList ++ x
          )
      )
  }

  private object CRUD {
    def exists: ID => ConnectionIO[Boolean] = id =>
      s"select ${fields.id} from $pgTable where ${fields.id} = '$id'".toFragment
        .query[String]
        .option
        .map(_.nonEmpty)

    def select: ID => ConnectionIO[Option[Folder]] =
      id =>
        s"""
           |select ${fields.id}, ${fields.name}, ${fields.ownerId}, ${fields.schema} from $pgTable
           |where ${fields.id} = '$id'
           |""".stripMargin.toFragment
          .query[(ID, String, ID, String)]
          .option
          .map(_.map(toFolder))

    def update: Folder => ConnectionIO[Int] = {
      case Folder(id, name, ownerId, schema) =>
        s"""
           |update $pgTable
           |set ${fields.name} = '$name', ${fields.ownerId} = '$ownerId', ${fields.schema} = '${Json.toJson(schema).toString().fixForSql}'
           |where ${fields.id} = '$id'
           |""".stripMargin.toFragment.update.run
    }

    def updateParts(id: ID, name: Option[String], ownerId: Option[String]): ConnectionIO[Int] = {
      val updateValues = List(
        name.map(fields.name + " = '" + _ + "'"),
        ownerId.map(fields.ownerId + " = '" + _ + "'")
        ).flatten.mkString(", ")

      s"""
         |update $pgTable
         |set $updateValues
         |where id = '$id'
         |""".stripMargin.toFragment.update.run
    }

    def insert: Folder => ConnectionIO[Int] = {
      case Folder(id, name, ownerId, schema) =>
        s"""
           |insert into $pgTable
           |(${fields.id}, ${fields.name}, ${fields.ownerId}, ${fields.schema})
           |values('$id', '${name.fixForSql}', '$ownerId', '${Json.toJson(schema).toString().fixForSql}')
           |""".stripMargin.toFragment.update.run
    }

    def delete: ID => ConnectionIO[Int] =
      id => s"""
               |delete from $pgTable
               |where ${fields.id} = '$id'
               |""".stripMargin.toFragment.update.run
  }

  implicit val folderJson: Format[Folder] = Json.format
}
