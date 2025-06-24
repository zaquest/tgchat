val scala3Version = "3.7.1"

val telegramiumVersion = "9.900.0"
val http4sVersion = "0.23.29"
val pureconfigVersion = "0.17.9"
val circeVersion = "0.14.8"
val fs2Version = "3.12.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "Telegram to LLM bot",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    scalacOptions ++= Seq(          // use ++= to add to existing options
      "-encoding", "utf8",          // if an option takes an arg, supply it on the same line
      "-explain",
      "-deprecation",
    ),

    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test,
    libraryDependencies ++= Seq(
      "io.github.apimorphism" %% "telegramium-core",
      "io.github.apimorphism" %% "telegramium-high",
    ).map(_ % telegramiumVersion),
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core", // fs2.compression.Compression
      "co.fs2" %% "fs2-io", // fs2.io.net.Network
    ).map(_ % fs2Version),
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-client",
      "org.http4s" %% "http4s-ember-server",
      "org.http4s" %% "http4s-dsl",
      "org.http4s" %% "http4s-circe",
    ).map(_ % http4sVersion),
    libraryDependencies += "com.github.pureconfig" %% "pureconfig-core"  % pureconfigVersion,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-literal",
    ).map(_ % circeVersion)
  )
