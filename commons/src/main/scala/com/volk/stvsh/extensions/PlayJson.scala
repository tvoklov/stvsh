package com.volk.stvsh.extensions

import play.api.libs.json.{ Format, Json }

object PlayJson {
  implicit class ToJson[T : Format](folder: T) {
    def toJson: String = Json.toJson(folder).toString()
  }
}
