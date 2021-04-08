package com.volk.stvsh.db

import cats.effect._
import com.volk.stvsh.BackendConfig.config
import com.volk.stvsh.db.Access.AccessType.AccessType
import com.volk.stvsh.db.Aliases._
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
    def perform: IO[T] = DBAccess.perform(io)
    def unsafeRunSync: T = DBAccess.perform(io).unsafeRunSync()
  }

  implicit class IdDbAccess(id: ID) {
    def getFolder: ConnectionIO[Option[Folder]] = Folder.get(id)
    def getUser: ConnectionIO[Option[User]] = User.get(id)
    def getSheet: ConnectionIO[Option[Sheet]] = Sheet.get(id)
  }

  implicit class FolderDbAccess(folder: Folder) {
    def save: ConnectionIO[Int] = Folder.save(folder)
    def getUsers: ConnectionIO[List[(User, List[AccessType])]] = Folder.getUsers(folder)
    def getSheets(offset: Option[Long] = None, limit: Option[Long] = None): ConnectionIO[List[Sheet]] =
      Sheet.findBy(Some(folder.id), offset, limit)
    def allow(user: User, accessTypes: List[AccessType]): ConnectionIO[Int] =
      Folder.allowAccess(user, accessTypes = accessTypes)(folder)
  }

  implicit class SheetDbAccess(sheet: Sheet) {
    def save: ConnectionIO[Int] = Sheet.save(sheet)
  }

  implicit class UserDbAccess(user: User) {
    def save: ConnectionIO[Int] = User.save(user)
    def getOwnedFolders: ConnectionIO[List[Folder]] = Folder.findBy(ownerId = Some(user.id))
  }

}
