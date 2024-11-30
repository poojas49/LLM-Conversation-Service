ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.textgeneration"

lazy val root = (project in file("."))
  .settings(
    name := "llm-server",

    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value
    ),

    libraryDependencies ++= {
      val AkkaVersion = "2.6.20"
      val AkkaHttpVersion = "10.2.10"

      Seq(
        // Akka
        "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
        "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,

        // JSON support
        "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
        "io.spray" %% "spray-json" % "1.3.6",

        // HTTP Client
        "com.softwaremill.sttp.client3" %% "core" % "3.9.0",

        // Protobuf
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",

        // Logging
        "ch.qos.logback" % "logback-classic" % "1.4.7",
        "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",

        // Testing
        "org.scalatest" %% "scalatest" % "3.2.15" % Test
      )
    }
  )