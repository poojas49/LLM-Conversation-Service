ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"

lazy val root = (project in file("."))
  .settings(
    name := "llm-rest-service",
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value
    ),

    // Assembly settings
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x if x.endsWith("/module-info.class") => MergeStrategy.discard
      case "META-INF/versions/9/module-info.class" => MergeStrategy.discard
      case "google/protobuf/struct.proto" => MergeStrategy.first
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x if x.contains("sigar") => MergeStrategy.first
      case x if x.contains("io.netty.versions.properties") => MergeStrategy.first
      case x if x.contains("module-info.class") => MergeStrategy.discard
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    },

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.5.0",
      "com.typesafe.akka" %% "akka-stream" % "2.8.0",
      "com.typesafe.akka" %% "akka-actor-typed" % "2.8.0",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "com.thesamet.scalapb" %% "scalapb-json4s" % "0.12.0",
      "com.softwaremill.sttp.client3" %% "core" % "3.8.13",
      "com.softwaremill.sttp.client3" %% "spray-json" % "3.8.13",
      "org.json4s" %% "json4s-native" % "4.0.6",
      "ch.qos.logback" % "logback-classic" % "1.4.7",
      "org.slf4j" % "slf4j-api" % "2.0.7",
      "org.scalatest" %% "scalatest" % "3.2.15" % Test
    )
  )