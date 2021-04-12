package com.volk.stvsh

import com.volk.stvsh.commons.AppConfig
import pureconfig.generic.auto._
import pureconfig.ConfigSource

object BackendConfig {

  lazy val config: BackendConfig = ConfigSource
    .fromConfig(AppConfig.getRoot.getConfig("backend"))
    .load[BackendConfig]
    .getOrElse(throw new IllegalStateException("could not read backend config"))

  case class BackendConfig(database: DatabaseConfig)

  case class DatabaseConfig(
      host: String,
      port: Int,
      user: String,
      password: String,
      database: String,
  ) {
    def url: String = s"jdbc:postgresql://$host:$port/$database"
  }

}
