package com.volk.stvsh.db.objects.folder

import cats.effect.IO
import cats.free.Free
import com.volk.stvsh.db.Aliases._
import com.volk.stvsh.db.objects.{ asFragment, User }
import com.volk.stvsh.db.objects.folder.Access.AccessType.AccessType
import com.volk.stvsh.db.objects.folder.Schema.FolderSchema
import com.volk.stvsh.extensions.CatsExtensions._
import com.volk.stvsh.extensions.DoobieExtensions._
import com.volk.stvsh.extensions.SqlUtils._
import doobie.free.connection.ConnectionOp
import doobie.implicits._
import doobie.{ ConnectionIO, Fragment }
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

  def get: ID => ConnectionIO[Option[Folder]] =
    CRUD
      .select(_)
      .query[(ID, String, ID, String)]
      .option
      .map(_.map(toFolder))

  def save: Folder => ConnectionIO[Int] = {
    case f @ Folder(id, _, _, _) =>
      get(id)
        .flatMap(
          _.fold(CRUD.insert(f))(
            _ => CRUD.update(f)
          ).update.run
        )
  }

  def delete: Folder => ConnectionIO[Int] = CRUD.delete(_).update.run

  def findBy(
      ownerId: Option[String] = None,
      userId: Option[String] = None,
  ): ConnectionIO[List[Folder]] = {
    val ownerUserFilter =
      List(
        ownerId.map(fields.ownerId + " = '" + _ + "'"),
        userId.map(s"${fields.id} in (select ${Access.fields.folderId} from ${Access.pgTable} where ${Access.fields.userId} = '" + _ + "')")
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

  def allowAccess(
      user: User,
      allow: Boolean = true,
      accessTypes: List[AccessType] = Nil,
  ): Folder => ConnectionIO[Int] = {
    case Folder(id, _, _, _) =>
      val insert = Access.CRUD.insert(id, user.id)

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
              else Access.CRUD.delete(id, user.id, o)

            sql.update.run
        }
  }

  def getUsers: Folder => ConnectionIO[List[(User, List[AccessType])]] = {
    case Folder(id, _, ownerId, _) =>
      val owner: Free[ConnectionOp, Option[(User, List[AccessType])]] =
        User
          .get(ownerId)
          .map(_.map(_ -> (Access.AccessType.full :: Nil)))

      val users = Access.getUsers(id)

      owner.flatMap(
        o =>
          users.map(
            x => o.toList ++ x
          )
      )
  }

  private object CRUD {
    def select: ID => Fragment = asFragment compose {
      id =>
        s"""
           |select ${fields.id}, ${fields.name}, ${fields.ownerId}, ${fields.schema} from $pgTable
           |where ${fields.id} = '$id'
           |""".stripMargin
    }

    def update: Folder => Fragment = asFragment compose {
      case Folder(id, name, ownerId, schema) =>
        s"""
           |update $pgTable
           |set ${fields.name} = '$name', ${fields.ownerId} = '$ownerId', ${fields.schema} = '${Json.toJson(schema).toString().fixForSql}'
           |where ${fields.id} = '$id'
           |""".stripMargin
    }

    def insert: Folder => Fragment = asFragment compose {
      case Folder(id, name, ownerId, schema) =>
        s"""
           |insert into $pgTable
           |(${fields.id}, ${fields.name}, ${fields.ownerId}, ${fields.schema})
           |values('$id', '${name.fixForSql}', '$ownerId', '${Json.toJson(schema).toString().fixForSql}')
           |""".stripMargin
    }

    def delete: Folder => Fragment = asFragment compose {
      case Folder(id, _, _, _) =>
        s"""
           |delete from $pgTable
           |where ${fields.id} = '$id'
           |""".stripMargin
    }
  }

  implicit class FolderJson(folder: Folder) {
    def toJson: String = Json.toJson(folder).toString()
  }

  implicit val folderJson: Format[Folder] = Json.format
}

object Access {
  private[db] val pgTable = "folder_access"

  private[db] object fields {
    val folderId   = "folder_id"
    val userId     = "user_id"
    val accessType = "access_type"
  }

  object AccessType {
    type AccessType = String

    val full  = "FULL"
    val read  = "READ"
    val write = "WRITE"
  }

  private[folder] object CRUD {
    def insert(folderId: ID, userId: ID): AccessType => Fragment =
      accessType =>
        asFragment {
          s"""
             |insert into $pgTable
             |(${fields.folderId}, ${fields.userId}, ${fields.accessType})
             |values('$folderId', '$userId', '$accessType')
             |""".stripMargin
        }

    def delete(folderId: ID, userId: ID, accessTypes: List[AccessType] = Nil): Fragment = asFragment {
      s"""
         |delete from $pgTable
         |where ${fields.folderId} = '$folderId' and ${fields.userId} = '$userId'
         |${accessTypes.mkString(s" and ${fields.accessType} in ('", "', '", "')")}
         |""".stripMargin
    }
  }

  def getFor(folderId: ID, userId: ID): doobie.ConnectionIO[List[AccessType]] = {
    val sql =
      s"""
         |select ${fields.accessType} from $pgTable
         |where ${fields.folderId} = '$folderId' and ${fields.userId} = '$userId'
         |""".stripMargin

    asFragment(sql).query[AccessType].to[List]
  }

  def getUsers(id: ID): ConnectionIO[List[(User, List[AccessType])]] = {
    val sql =
      s"""
         |select ${fields.userId}, ${fields.accessType} from $pgTable
         |where folder_id = '$id'
         |""".stripMargin

    asFragment(sql)
      .query[(ID, String)]
      .to[List]
      .flatMap(
        _.groupMap(_._1)(_._2)
          .map {
            case (id, accessTypes) => User.get(id).map(_ -> accessTypes)
          }
          .sequence
          .map(_.flatMap {
            case (ost, atps) => ost.map(_ -> atps)
          })
      )
  }

}
