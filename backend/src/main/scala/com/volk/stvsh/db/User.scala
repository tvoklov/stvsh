package com.volk.stvsh.db

import com.volk.stvsh.db.Aliases._
import com.volk.stvsh.extensions.SqlUtils.SqlFixer

case class User(
    id: ID,
    username: String,
)

object User {
  import doobie._
  import doobie.implicits._

  import java.util.UUID

  private[db] val pgTable = "users"

  private[db] object fields {
    val id = "id"
    val username = "username"
  }

  def apply(username: String): User = User(UUID.randomUUID().toString, username)

  private def toUser: ((ID, String)) => User = { case (id, username) =>
    User(id, username)
  }

  def get(id: String): doobie.ConnectionIO[Option[User]] = {
    val sql =
      sql"select ${fields.id}, ${fields.username} from " ++ Fragment.const(pgTable) ++
        sql" where " ++ Fragment.const(fields.id) ++ sql" = $id"

    sql
      .query[(ID, String)]
      .option
      .map(_.map(toUser))
  }

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

  def save: User => ConnectionIO[Int] = { case User(id, username) =>
    val sql =
      s"""
         |insert into $pgTable
         |(${fields.id}, ${fields.username})
         |values('$id', '${username.fixForSql}')
         |""".stripMargin

    Fragment.const(sql).update.run
  }

}
