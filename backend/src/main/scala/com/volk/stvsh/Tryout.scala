package com.volk.stvsh

import cats.effect.IO
import com.volk.stvsh.db.{ Folder, Sheet, User }
import com.volk.stvsh.db.Access.AccessType
import com.volk.stvsh.db.DBAccess._
import com.volk.stvsh.db.Fields.{ Image, Text }
import com.volk.stvsh.db.Schema.ValueType

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
    gotten <- "7fd6d29f-4a20-4b67-9b7a-5f6cee8b136c".getFolder.perform
    users <- gotten.map(_.getUsers).map(_.perform).getOrElse(IO.pure(Nil))
  } yield users

  val p3 = "cfb5422f-8143-4337-a00b-d12565fc027e".getSheet.perform



//  val folder -> allowed = p1.unsafeRunSync()
//
//  println(folder)
//  println(allowed)

  println(p1.unsafeRunSync())
  println(p3.unsafeRunSync())


}
