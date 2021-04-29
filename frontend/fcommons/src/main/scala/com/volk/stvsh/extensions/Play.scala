package com.volk.stvsh.extensions

import cats.effect.IO
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError

import scala.concurrent.Future

object Play {

  def ioToFuture: IO[Result] => Future[Result] =
    _.attempt
      .map {
        case Right(value) => value
        case Left(value) =>
          value.printStackTrace()
          InternalServerError(value.getMessage)
      }
      .unsafeToFuture()

  implicit class PlayIO(io: IO[Result]) {
    def toResultFuture: Future[Result] = ioToFuture(io)
  }
}
