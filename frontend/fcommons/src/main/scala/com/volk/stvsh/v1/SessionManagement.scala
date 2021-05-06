package com.volk.stvsh.v1

import cats.implicits.catsSyntaxApplicativeId
import com.volk.stvsh.HeaderNames
import com.volk.stvsh.db.Aliases.ID
import com.volk.stvsh.db.objects.UserSession
import com.volk.stvsh.db.objects.folder.FolderAccess
import com.volk.stvsh.db.objects.folder.FolderAccess.AccessType
import doobie.ConnectionIO
import play.api.mvc.Results.{BadRequest, Forbidden}
import play.api.mvc.{AnyContent, Request, Result}

object SessionManagement {

  def withSession[T](func: UserSession => ConnectionIO[Result]): Request[AnyContent] => ConnectionIO[Result] =
    _.headers.get(HeaderNames.session) match {
      case Some(sid) =>
        UserSession.get(sid) match {
          case Some(us) => func(us)
          case None     => BadRequest("session does not exist").pure[ConnectionIO]
        }
      case None => BadRequest("no session").pure[ConnectionIO]
    }

  def withSessionCheck(check: UserSession => ConnectionIO[Boolean], checkError: String = "Can not do this with current session")(
      func: Request[AnyContent] => ConnectionIO[Result]
  ): Request[AnyContent] => ConnectionIO[Result] =
    request =>
      withSession {
        us =>
          for {
            f   <- check(us)
            res <- if (f) func(request) else Forbidden(checkError).pure[ConnectionIO]
          } yield res
      }(request)

  def ifHasFolderAccess(accessTypes: AccessType*)(folderId: ID)(
      ifHasAccess: Request[AnyContent] => ConnectionIO[Result],
      otherwise: String = "no access to folder"
  ): Request[AnyContent] => ConnectionIO[Result] =
    withSessionCheck(us => FolderAccess.hasAccessType(folderId)(us.userId)(accessTypes: _*).pure[ConnectionIO], otherwise)(ifHasAccess)

}
