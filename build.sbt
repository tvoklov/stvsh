name := "stvsh"
version := "0.1"
scalaVersion := "2.13.5"
idePackagePrefix := Some("com.volk.stvsh")

val libraries = new {
  val versions = new {
    val pureConfig = "0.14.1"
    val skunk = "0.0.24"
    val play = "2.8.7"

    val doobie = new {
      val core = "0.12.1"
      val postgres = core
    }

    val cats = new {
      val core = "2.3.0"
      val effect = "2.4.0"
      val mouse = "1.0.0"
      val kittens = "2.2.1"
      val collections = "0.9.0"
    }
  }

  val pureConfig = "com.github.pureconfig" %% "pureconfig" % versions.pureConfig

  val skunk = "org.tpolecat" %% "skunk-core" % versions.skunk
  val doobieCore = "org.tpolecat" %% "doobie-core" % versions.doobie.core
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % versions.doobie.postgres

  val play = "com.typesafe.play" %% "play" % versions.play

  val cats = "org.typelevel" %% "cats-core" % versions.cats.core
  val catsEffect = "org.typelevel" %% "cats-effect" % versions.cats.effect
  val mouse = "org.typelevel" %% "mouse" % versions.cats.mouse
  val kittens = "org.typelevel" %% "kittens" % versions.cats.kittens
  val catsCollections = "org.typelevel" %% "cats-collections-core" % versions.cats.collections

  val configs = pureConfig :: Nil
  val doobie = doobieCore :: doobiePostgres :: Nil
}

resolvers += Resolver.mavenCentral

// libraries i can't have anywhere else
lazy val commons = (project in file("commons"))
  .settings(
    libraryDependencies ++= libraries.configs ++
      Seq(
        libraries.cats,
        libraries.catsEffect,
        libraries.mouse,
        libraries.kittens,
        libraries.catsCollections
      )
  )

// backend functions
lazy val backend = (project in file("backend"))
  .settings(
    libraryDependencies ++= libraries.doobie
  )
  .dependsOn(commons)

// frontend implementations: a rest api and a webui
lazy val api = (project in file("frontend/api"))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      libraries.play
    )
  )
  .dependsOn(backend)

lazy val server = (project in file("frontend/webui"))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      libraries.play
    )
  )
  .dependsOn(backend)
