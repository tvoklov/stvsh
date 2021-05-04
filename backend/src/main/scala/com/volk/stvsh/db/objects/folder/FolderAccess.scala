package com.volk.stvsh.db.objects.folder

import cats.free.Free
import com.volk.stvsh.db.objects._
import com.volk.stvsh.db.Aliases._
import com.volk.stvsh.extensions.Cats._
import com.volk.stvsh.extensions.Doobie._
import doobie._
import doobie.free.connection

object FolderAccess {
  private[db] val pgTable = "folder_access"

  private[db] object fields {
    val folderId   = "folder_id"
    val userId     = "user_id"
    val accessType = "access_type"
  }

  trait AccessType { def value: String }
  object AccessType {
    def apply(at: String): AccessType = at match {
      case CanRead.value  => CanRead
      case CanWrite.value => CanWrite
      case CanEdit.value  => CanEdit
    }
  }

  case object CanRead  extends AccessType { val value = "READ"  }
  case object CanWrite extends AccessType { val value = "WRITE" }
  case object CanEdit  extends AccessType { val value = "EDIT"  }

  def accessTypesFor(folderId: ID)(userId: ID): doobie.ConnectionIO[List[AccessType]] =
    accessTypesForQuery(folderId)(userId).to[List]

  def accessTypesForQuery(folderId: ID)(userId: ID): Query0[AccessType] = asFragment(
    s"""
       |select ${fields.accessType} from $pgTable
       |where ${fields.folderId} = '$folderId' and ${fields.userId} = '$userId'
       |""".stripMargin
  ).query[String].map(AccessType(_))

  def hasAccessType(folderId: ID)(userId: ID)(_type: AccessType): ConnectionIO[Boolean] = {
    val sql =
      s"""
         |select ${fields.accessType} from $pgTable
         |where ${fields.folderId} = '$folderId' and ${fields.userId} = '$userId' and ${fields.accessType} = '${_type.value}'
         |""".stripMargin

    asFragment(sql)
      .query[String]
      .option
      .map(_.nonEmpty)
  }

  /** @return users with any access to the folder other than the owner */
  def getUsersWithAccess(folderId: ID): ConnectionIO[List[(User, List[AccessType])]] = {
    val sql =
      s"""
         |select ${fields.userId}, ${fields.accessType} from $pgTable
         |where folder_id = '$folderId'
         |""".stripMargin

    for {
      ats <- asFragment(sql).query[(ID, String)].to[List]
      maybeUsersWithTypes <- ats
        .groupMap(_._1)(_._2)
        .map {
          case (id, accessTypes) => User.get(id).map(_ -> accessTypes)
        }
        .sequence
    } yield maybeUsersWithTypes.collect {
      case (Some(user), types) => user -> types.map(AccessType(_))
    }
  }

  /** gives/revokes access of given types to the user.
    * will not touch other types of access given to user.
    */
  def allowAccess(
      userId: ID,
      allow: Boolean = true,
      accessTypes: List[AccessType],
  ): Folder => ConnectionIO[Int] = {
    case Folder(id, _, _, _) =>
      val insert = CRUD.insert(id, userId)

      FolderAccess
        .accessTypesFor(id)(userId)
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
              else CRUD.delete(id, userId, o)

            sql.update.run
        }
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

}
