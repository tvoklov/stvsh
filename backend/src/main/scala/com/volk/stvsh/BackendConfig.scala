package com.volk.stvsh

object BackendConfig {

  case class BackendConfig(
                          databaseConfig: DatabaseConfig
                   )

  case class DatabaseConfig(
                           host: String,
                           port: Int,
                           user: String,
                           password: String,
                           database: String,

                           )

//  val config: BackendConfig = _

}
