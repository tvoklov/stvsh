package v1.user

import cats.effect.IO
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.folder.Folder._
import play.api.libs.json.Json
import play.api.mvc.{ AbstractController, Action, AnyContent, ControllerComponents }

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

}
