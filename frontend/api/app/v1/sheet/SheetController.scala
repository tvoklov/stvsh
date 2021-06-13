package v1.sheet

import cats.implicits._
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.{ Sheet, SheetField }
import com.volk.stvsh.db.objects.SheetField.SheetField
import com.volk.stvsh.db.objects.folder.Folder
import com.volk.stvsh.db.objects.folder.FolderAccess.{ CanReadSheets, CanWriteSheets }
import com.volk.stvsh.db.objects.folder.Schema.{ Key, ValueType }
import com.volk.stvsh.db.Aliases.{ ID, SheetValues }
import com.volk.stvsh.extensions.Play.{ ActionBuilderOps, EitherResultable }
import com.volk.stvsh.extensions.PlayJson._
import com.volk.stvsh.v1.SessionManagement._
import doobie.ConnectionIO
import doobie.implicits._
import play.api.libs.json.{ Format, Json }
import play.api.mvc._

import java.util.UUID
import javax.inject.Inject

class SheetController @Inject() (val cc: ControllerComponents) extends AbstractController(cc) {

  def get: String => Action[AnyContent] = id =>
    Action.asyncF {
      request =>
        val cio = for {
          maybeSheet <- id.getSheet
          res <- maybeSheet match {
            case None => NotFound("").pure[ConnectionIO]
            case Some(sheet) =>
              ifHasFolderAccess(CanReadSheets)(sheet.folderId)(
                _ => Ok(sheet.toJson).pure[ConnectionIO]
              )(request)
          }
        } yield res

        cio.perform
    }

  def update: String => Action[AnyContent] = id =>
    Action.asyncF {
      req =>
        val cio = for {
          maybeSheet <- id.getSheet
          res <-
            maybeSheet match {
              case None => NotFound("").pure[ConnectionIO]
              case Some(existingSheet) =>
                ifHasFolderAccess(CanWriteSheets)(existingSheet.folderId)(
                  _.body.asJson.flatMap(_.asOpt[PreSaveSheet]) match {
                    case None          => BadRequest("Bad json").pure[ConnectionIO]
                    case Some(preSave) => saveSheet(existingSheet.folderId, Some(id))(preSave)
                  }
                )(req)
            }
        } yield res

        cio.perform
    }

  def create: String => Action[AnyContent] = folderId =>
    Action.asyncF {
      ifHasFolderAccess(CanWriteSheets)(folderId)(
        _.body.asJson.map(_.as[PreSaveSheet]) match {
          case None               => BadRequest("bad json value for a sheet").pure[ConnectionIO]
          case Some(preSaveSheet) => saveSheet(folderId)(preSaveSheet)
        }
      ) andThen (_.perform)
    }

  def setArchive(bool: Boolean): String => Action[AnyContent] = id =>
    Action.asyncF {
      request =>
        val cio = for {
          maybeSheet <- id.getSheet
          res <- maybeSheet match {
            case None => NotFound("").pure[ConnectionIO]
            case Some(sheet) =>
              ifHasFolderAccess(CanWriteSheets)(sheet.folderId)(
                _ => for { _ <- Sheet.setArchived(bool)(sheet) } yield Ok(sheet.copy(isArchived = bool).toJson)
              )(request)
          }
        } yield res

        cio.perform
    }

  def delete: String => Action[AnyContent] = id =>
    Action.asyncF {
      req =>
        val cio = for {
          maybeSheet <- id.getSheet
          res <- maybeSheet match {
            case None => NotFound("").pure[ConnectionIO]
            case Some(sheet) =>
              ifHasFolderAccess(CanReadSheets)(sheet.folderId)(
                _ => for { _ <- Sheet.delete(sheet) } yield Ok(sheet.id)
              )(req)
          }
        } yield res

        cio.perform
    }

  /** creates a new sheet using given PreSaveSheet's values and ID as folder in which sheet is created.
    * technically also checks that sheet follows folder structure (for more info see implementation of [[PreSaveSheet]]'s [[toSheet]])
    * @return sheet already wrapped in an Ok(), or an error (aka BadRequest)
    */
  def saveSheet(folderId: ID, sheetId: Option[ID] = None): PreSaveSheet => ConnectionIO[Result] =
    pss => {
      for {
        folderOpt <- folderId.getFolder
        res <-
          folderOpt match {
            case None =>
              BadRequest(s"no folder with id $folderId").pure[ConnectionIO]
            case Some(folder) =>
              pss.toSheet(sheetId.getOrElse(UUID.randomUUID().toString), folder = folder) match {
                case Left(error)  => BadRequest(error).pure[ConnectionIO]
                case Right(sheet) => for { _ <- sheet.save } yield Ok(sheet.toJson)
              }
          }
      } yield res
    }

}
