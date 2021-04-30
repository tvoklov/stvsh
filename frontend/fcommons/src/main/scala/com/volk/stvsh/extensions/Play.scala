package com.volk.stvsh.extensions

import cats.effect.{Effect, IO}
import play.api.mvc.{Action, ActionBuilder, AnyContent, BodyParser, Result}
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

  // stolen from https://github.com/larousso/play-cats-io
  // modified a little bit
  implicit class ActionBuilderOps[+R[_], B](ab: ActionBuilder[R, B]) {
    import cats.implicits._
    import cats.effect.implicits._

    def asyncF[F[_]: Effect](cb: R[B] => F[Result]): Action[B] =
      ab.async(cb(_).toIO.toResultFuture)

    def asyncF[F[_]: Effect](cb: => F[Result]): Action[AnyContent] =
      ab.async(cb.toIO.toResultFuture)

    def asyncF[F[_]: Effect, A](bp: BodyParser[A])(cb: R[A] => F[Result]): Action[A] =
      ab.async[A](bp)(cb(_).toIO.unsafeToFuture())
  }
}
