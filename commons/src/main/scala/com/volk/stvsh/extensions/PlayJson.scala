package com.volk.stvsh.extensions

import play.api.libs.json.{ Format, Json, JsValue }

object PlayJson {
  implicit class ToJson[T : Format](anything: T) {
    def toJson: JsValue = Json.toJson(anything)
    def toJsonString: String = Json.toJson(anything).toString()
  }
}
