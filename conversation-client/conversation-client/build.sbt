ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "conversation-client"
  )

libraryDependencies ++= Seq(
  "io.github.ollama4j" % "ollama4j" % "1.0.79",
  "com.softwaremill.sttp.client3" %% "core" % "3.8.13",
  "com.softwaremill.sttp.client3" %% "play-json" % "3.8.13",
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.typesafe" % "config" % "1.4.2"
)