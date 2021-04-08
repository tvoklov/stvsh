package com.volk.stvsh.db

import cats._
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._

object DBAccess {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",     // driver classname
    "jdbc:postgresql://localhost:5432/jilfond",     // connect URL (driver-specific)
    "postgres",                  // user
    "postgres",                          // password
    Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
  )

  val p1 = sql"select id from address".query[Int].to[List]

  println(p1.transact(xa).unsafeRunSync())



}
