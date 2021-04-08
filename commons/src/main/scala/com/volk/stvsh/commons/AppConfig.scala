package com.volk.stvsh.commons

import com.typesafe.config.{ Config, ConfigFactory }

import java.io.File

object AppConfig {

  private var configRoot: Config = _

  def getRoot: Config =
    if (configRoot == null)
      setRoot()
    else
      configRoot

  def setRoot(files: File*): Config = {
    if (configRoot == null)
      configRoot = files
        .foldLeft(ConfigFactory.empty())((cfg, f) => cfg.withFallback(ConfigFactory.parseFile(f)))
        .withFallback(ConfigFactory.load())
    configRoot
  }

}
