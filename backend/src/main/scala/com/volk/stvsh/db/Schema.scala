package com.volk.stvsh.db

object Schema {

  type FolderSchema = Map[Key, ValueType]

  type Key = String

  type ValueType = String

  object ValueType {
    val text        = "text"
    val wholeNumber = "wholeNumber"
    val floatingPointNumber = "floatingPointNumber"
    val image       = "image"
    val tags   = "tags"
  }

}
