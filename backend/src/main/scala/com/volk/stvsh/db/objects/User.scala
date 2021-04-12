package com.volk.stvsh.db.objects

import com.volk.stvsh.db.Aliases._
import com.volk.stvsh.extensions.SqlUtils.SqlFixer
import play.api.libs.json.{ Format, Json }

case class User(
    id: ID,
    username: String,
)

object User {
  import doobie._

  import java.util.UUID

  private[db] val pgTable = "users"

  private[db] object fields {
    val id       = "id"
    val username = "username"
  }

  def apply(username: String): User = User(UUID.randomUUID().toString, username)

  private def toUser: ((ID, String)) => User = {
    case (id, username) =>
      User(id, username)
  }

  def get: ID => ConnectionIO[Option[User]] =
    CRUD
      .select(_)
      .query[(ID, String)]
      .option
      .map(_.map(toUser))

  def save: User => ConnectionIO[Int] = {
    case u @ User(id, _) =>
      get(id).flatMap(
        _.fold(CRUD.insert(u))(
          _ => CRUD.update(u)
        ).update.run
      )
  }

  def delete: User => ConnectionIO[Int] = CRUD.delete(_).update.run

  def findBy(username: Option[String]): ConnectionIO[List[User]] = {
    val filters =
      List(
        username.map(_.fixForSql).map(fields.username + " = '" + _ + "'")
      ).flatten.mkString("where ", " and ", "")

    val sql =
      s"""
         |select ${fields.id}, ${fields.username} from $pgTable
         |$filters
         |""".stripMargin

    Fragment
      .const(sql)
      .query[(ID, String)]
      .to[List]
      .map(_.map(toUser))
  }

  private object CRUD {
    def select: ID => Fragment = asFragment compose {
      id =>
        s"""
           |select ${fields.id}, ${fields.username} from $pgTable
           |where ${fields.id} = '$id'
           |""".stripMargin
    }

    def insert: User => Fragment = asFragment compose {
      case User(id, username) =>
        s"""
           |insert into $pgTable
           |(${fields.id}, ${fields.username})
           |values('$id', '${username.fixForSql}')
           |""".stripMargin
    }

    def update: User => Fragment = asFragment compose {
      case User(id, username) =>
        s"""
           |update $pgTable
           |set ${fields.username} = '$username'
           |where ${fields.id} = '$id'
           |""".stripMargin
    }

    def delete: User => Fragment = asFragment compose {
      case User(id, _) =>
        s"""
           |delete from $pgTable
           |where id = '$id'
           |""".stripMargin
    }
  }

  implicit class UserJson(user: User) {
    def toJson: String = Json.toJson(user).toString()
  }

  implicit val userJson: Format[User] = Json.format
}
