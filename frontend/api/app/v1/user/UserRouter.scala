package v1.user

import play.api.routing.Router.Routes
import play.api.routing.sird._
import play.api.routing.SimpleRouter

import javax.inject.Inject

class UserRouter @Inject()(controller: UserController) extends SimpleRouter {
  override def routes: Routes = {
    case GET(p"/$id/folders") =>
      controller.getFolders(id)

    case POST(p"") =>
      controller.post
  }
}
