package v1.user

import cats.implicits._
import com.volk.stvsh.db.Aliases.ID
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.User
import com.volk.stvsh.db.objects.folder.Folder._
import com.volk.stvsh.extensions.Play.ActionBuilderOps
import com.volk.stvsh.extensions.PlayJson._
import doobie.ConnectionIO
import play.api.libs.json.{ Format, Json }
import play.api.mvc.{ AbstractController, Action, AnyContent, ControllerComponents }

import java.util.UUID
import javax.inject.Inject

class UserController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {

  def get(id: String): Action[AnyContent] = Action.asyncF {
    id.getUser.map {
      case None       => NotFound("no user found for given id")
      case Some(user) => Ok(user.toJson)
    }.perform
  }

  def getFolders(id: String): Action[AnyContent] = Action.asyncF {
    val cio = for {
      maybeUser <- id.getUser
      res <- maybeUser match {
        case None       => NotFound("no user found for given id").pure[ConnectionIO]
        case Some(user) => user.getAccessibleFolders.map(_.toJson).map(Ok(_))
      }
    } yield res

    cio.perform
  }

  def post: Action[AnyContent] = Action.asyncF {
    request =>
      val io = request.body.asJson.map(_.as[PreSaveUser]) match {
        case None => BadRequest("bad json value").pure[ConnectionIO]
        case Some(newUser) =>
          val user = newUser.toUser
          for { _ <- user.save } yield Ok(user.toJson)
      }

      io.perform
  }

}

case class PreSaveUser(id: Option[ID], username: String) {
  def toUser: User = User(id.getOrElse(UUID.randomUUID().toString), username)
}

object PreSaveUser {
  implicit val preSaveUserJson: Format[PreSaveUser] = Json.format
}
