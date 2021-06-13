package com.volk.stvsh.db

import cats.effect._
import com.volk.stvsh.BackendConfig.config
import com.volk.stvsh.db.Aliases._
import com.volk.stvsh.db.objects._
import com.volk.stvsh.db.objects.folder._
import com.volk.stvsh.db.objects.folder.FolderAccess.AccessType
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux

object DBAccess {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  private val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    config.database.url,
    config.database.user,
    config.database.password,
    Blocker.liftExecutionContext(ExecutionContexts.synchronous)
  )

  def perform[T]: ConnectionIO[T] => IO[T] = _.transact(xa)

  implicit class ConnectionIODbAccess[T](io: ConnectionIO[T]) {
    def perform: IO[T]   = DBAccess.perform(io)
    def unsafeRunSync: T = DBAccess.perform(io).unsafeRunSync()
  }

  implicit class IdDbAccess(id: ID) {
    def getFolder: ConnectionIO[Option[Folder]] = Folder.get(id)
    def getUser: ConnectionIO[Option[User]]     = User.get(id)
    def getSheet: ConnectionIO[Option[Sheet]]   = Sheet.get(id)
  }

  implicit class FolderDbAccess(folder: Folder) {
    def save: ConnectionIO[Int]                                = Folder.save(folder)
    def delete: ConnectionIO[Int]                              = Folder.delete(folder.id)
    def getOwner: ConnectionIO[Option[User]]                   = User.get(folder.ownerId)
    def getUsers: ConnectionIO[List[(User, List[AccessType])]] = Folder.getAllUsersWithAccess(folder)

    def getSheets(offset: Option[Long] = None, limit: Option[Long] = None, archived: Option[Boolean] = None): ConnectionIO[List[Sheet]] =
      Sheet.findBy(Some(folder.id), offset, limit, archived)

    def allow(user: User, accessTypes: List[AccessType]): ConnectionIO[Int] =
      FolderAccess.allowAccess(user.id, accessTypes = accessTypes)(folder)
  }

  implicit class SheetDbAccess(sheet: Sheet) {
    def save: ConnectionIO[Int]   = Sheet.save(sheet)
    def delete: ConnectionIO[Int] = Sheet.delete(sheet)
  }

  implicit class UserDbAccess(user: User) {
    def save: ConnectionIO[Int]                          = User.save(user)
    def delete: ConnectionIO[Int]                        = User.delete(user)
    def getOwnedFolders: ConnectionIO[List[Folder]]      = Folder.findBy(ownerId = Some(user.id))
    def getAccessibleFolders: ConnectionIO[List[Folder]] = Folder.findBy(ownerId = Some(user.id), userId = Some(user.id))
  }

}
