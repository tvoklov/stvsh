package com.volk.stvsh.extensions

import cats.free.Free

object Cats {

  def sequence[T[_], A](frees: Iterable[Free[T, A]]): Free[T, List[A]] =
    frees.foldLeft(Free.pure[T, List[A]](Nil))((prev, curr) => prev.flatMap(p => curr.map(_ :: p)))

  def sequence[T[_], A](opt: Option[Free[T, A]]): Free[T, Option[A]] =
    opt.fold[Free[T, Option[A]]](Free.pure(None))(_.map(Some(_)))

  implicit class SequenceExtension[T[_], A](frees: Iterable[Free[T, A]]) {
    def sequence: Free[T, List[A]] = Cats.sequence(frees)
  }

  implicit class OptionExtension[T[_], A](opt: Option[Free[T, A]]) {
    def sequence: Free[T, Option[A]] = Cats.sequence(opt)
  }

}
