package com.volk.stvsh

import cats.effect.IO
import com.volk.stvsh.db.objects.User
import com.volk.stvsh.db.objects.folder.Access.AccessType
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.objects.SheetField.{Image, Text}
import com.volk.stvsh.db.objects.Sheet
import com.volk.stvsh.db.objects.folder.Folder
import com.volk.stvsh.db.objects.folder.Schema.ValueType

object Tryout extends App {

  val p1 =
    for {
      user <- IO.pure(User("volk"))
      _    <- user.save.perform
      folder <- IO.pure(
        Folder(
          "Hello world",
          user,
          Map("cool" -> ValueType.text, "epic" -> ValueType.image)
        )
      )
      _       <- folder.save.perform

      user2 <- IO.pure(User("less cool user"))
      _ <- user2.save.perform
      _ <- folder.allow(user2, List(AccessType.read)).perform
      sheet <- IO.pure(Sheet(folder, Map("cool" -> Text("yeah it's cool"), "epic" -> Image("yiff.png"))))
      _ <- sheet.save.perform
      sheets <- folder.getSheets().perform
    } yield sheets

  val p2 = for {
    gotten <- "ce5d393f-669b-47fa-8172-2628a71d6360".getFolder.perform
    users <- gotten.map(_.getUsers).map(_.perform).getOrElse(IO.pure(Nil))
  } yield users

  val p3 = "0eed7b1d-63a9-47c8-8198-416c49a675ab".getSheet.perform



//  val folder -> allowed = p1.unsafeRunSync()
//
//  println(folder)
//  println(allowed)

  println(p3.unsafeRunSync())
//  println(p3.unsafeRunSync())


}
