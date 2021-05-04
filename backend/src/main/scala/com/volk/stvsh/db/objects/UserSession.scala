package com.volk.stvsh.db.objects

import cats.effect.IO
import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }
import com.volk.stvsh.db.Aliases.ID
import com.volk.stvsh.db.DBAccess._
import play.api.libs.json.{ Format, Json }

import java.time.LocalDateTime
import scala.concurrent.duration.DurationInt

case class UserSession(
    id: ID,
    userId: ID,
    expiryDate: LocalDateTime
)

object UserSession {
  import doobie._

  private[db] val pgTable = "sessions"

  private object fields {
    val id         = "id"
    val userId     = "user_id"
    val expiryDate = "expiry_date"
  }

  private val sessionsCache: LoadingCache[String, Option[UserSession]] =
    Scaffeine()
      .expireAfterWrite(1.hour)
      .maximumSize(100)
      .build((id: String) => getFromDb(id).perform.unsafeRunSync())

  def get: String => Option[UserSession] =
    id => sessionsCache.get(id)

  private def getFromDb: String => ConnectionIO[Option[UserSession]] =
    CRUD.get andThen (_.option)

  def create: UserSession => ConnectionIO[Int] =
    CRUD.create andThen (_.run)

  def delete: String => ConnectionIO[Int] =
    CRUD.delete andThen (_.run)

  object CRUD {
    import doobie._
    import doobie.implicits._
    import doobie.implicits.javasql._
    import doobie.implicits.javatimedrivernative._

    def get: String => Query0[UserSession] = id => asFragment {
      s"""
         |select ${fields.id}, ${fields.userId}, ${fields.expiryDate} from $pgTable
         |where ${fields.id} = '$id'
         |""".stripMargin
    }.query[(ID, ID, LocalDateTime)].map {
      case (id, uId, expiryDate) => UserSession(id, uId, expiryDate)
    }

    def create: UserSession => Update0 = {
      case UserSession(sessionId, userId, expiryDate) =>
        asFragment(
          s"""
             |insert into $pgTable
             |(${fields.id}, ${fields.userId}, ${fields.expiryDate})
             |values('$sessionId', '$userId', '${expiryDate.formatted("yyyy-MM-dd HH:mm:ss")}
             |""".stripMargin
        ).update
    }

    def delete: String => Update0 = id =>
      asFragment(
        s"""
           |delete from $pgTable where ${fields.id} = '$id'
           |""".stripMargin
      ).update

  }

  implicit val jsonFormat: Format[UserSession] = Json.format

}
