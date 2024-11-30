ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "llm-server"
  )
val akkaVersion = "2.8.0"
val akkaHttpVersion = "10.5.0"
val sttpVersion = "3.9.0"

libraryDependencies ++= Seq(
  // Akka
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,

  // STTP for HTTP client
  "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "json4s" % sttpVersion,

  // JSON handling
  "org.json4s" %% "json4s-native" % "4.0.6",
  "org.json4s" %% "json4s-jackson" % "4.0.6",

  // Config
  "com.typesafe" % "config" % "1.4.2",

  // Logging
  "ch.qos.logback" % "logback-classic" % "1.4.11",

  // Testing
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)