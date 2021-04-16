package v1.user

import cats.effect.IO
import com.volk.stvsh.db.Aliases.ID
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.User
import com.volk.stvsh.db.objects.folder.Folder._
import com.volk.stvsh.extensions.PlayJson._
import play.api.libs.json.{ Format, Json }
import play.api.mvc.{ AbstractController, Action, AnyContent, ControllerComponents }

import java.util.UUID
import javax.inject.Inject

class UserController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {

  def getFolders(id: String): Action[AnyContent] = Action.async {
    val io = for {
      maybeUser <- id.getUser.perform
      res <- maybeUser.fold(IO.pure(NotFound("no user found for given id"))) {
        user =>
          user.getAccessibleFolders.perform
            .map(_.map(_.toJson))
            .map(Json.toJson(_).toString())
            .map(Ok(_))
      }
    } yield res

    io.unsafeToFuture()
  }

  def post: Action[AnyContent] = Action.async {
    request =>
      val io = request.body.asJson
        .map(_.as[PreSaveUser])
        .fold(IO.pure(BadRequest("bad json value"))) {
          newUser =>
            val user = newUser.toUser
            for { _ <- user.save.perform } yield Ok(user.toJson)
        }

      io.unsafeToFuture()
  }

}

case class PreSaveUser(id: Option[ID], username: String) {
  def toUser: User = User(id.getOrElse(UUID.randomUUID().toString), username)
}

object PreSaveUser {

  implicit val preSaveUserJson: Format[PreSaveUser] = Json.format
}
