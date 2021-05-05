package v1.folder

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

import javax.inject.Inject

class FolderRouter @Inject() (controller: FolderController) extends SimpleRouter {
  override def routes: Routes = {
    case GET(p"/$id") =>
      controller.get(id)

//    case HEAD(p"/$id/sheets") =>
//      controller.head(id)

    case GET(p"/$id/sheets" ? q_o"sortBy=$sortBy" ? q_o"offset=$offset" ? q_o"sortBy=$limit") =>
      controller.getSheets(id)(offset.flatMap(_.toLongOption), limit.flatMap(_.toLongOption), sortBy)

    case POST(p"") =>
      controller.create
  }
}
