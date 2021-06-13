package v1.sheet

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

import javax.inject.Inject

class SheetRouter @Inject() (controller: SheetController) extends SimpleRouter {
  override def routes: Routes = {
    case GET(p"/$id") =>
      controller.get(id)

    case POST(p"/" ? q"folderId=$folderId") =>
      controller.create(folderId)

    case PUT(p"/$id") =>
      controller.update(id)

    // archiving a sheet
    case PUT(p"/archive/$id") =>
      controller.setArchive(true)(id)

    // dearchiving a sheet
    case DELETE(p"/archive/$id") =>
      controller.setArchive(false)(id)

    // fully removing a sheet
    case DELETE(p"/$id") =>
      controller.delete(id)
  }
}
