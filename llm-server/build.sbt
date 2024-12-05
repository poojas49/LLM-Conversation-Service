ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"

// Define versions
val AkkaVersion = "2.8.0"
val AkkaHttpVersion = "10.5.0"
val Slf4jVersion = "2.0.7"  // Match your current version
val LogbackVersion = "1.4.7" // Match your current version

lazy val root = (project in file("."))
  .settings(
    name := "llm-rest-service",

    // Force specific versions for logging dependencies
    dependencyOverrides ++= Seq(
      "org.slf4j" % "slf4j-api" % Slf4jVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "ch.qos.logback" % "logback-core" % LogbackVersion
    ),

    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value
    ),

    libraryDependencies ++= Seq(
      // Akka dependencies
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion excludeAll(
        ExclusionRule(organization = "org.slf4j")
        ),
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion excludeAll(
        ExclusionRule(organization = "org.slf4j")
        ),
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion excludeAll(
        ExclusionRule(organization = "org.slf4j")
        ),
      "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,

      // Other dependencies
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "com.thesamet.scalapb" %% "scalapb-json4s" % "0.12.0",
      "com.softwaremill.sttp.client3" %% "core" % "3.8.13",
      "com.softwaremill.sttp.client3" %% "spray-json" % "3.8.13",
      "org.json4s" %% "json4s-native" % "4.0.6",

      // Logging dependencies
      "org.slf4j" % "slf4j-api" % Slf4jVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion excludeAll(
        ExclusionRule(organization = "org.slf4j")
        ),

      // Testing
      "org.scalatest" %% "scalatest" % "3.2.15" % Test
    ),

    // Assembly settings
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x if x.endsWith("/module-info.class") => MergeStrategy.discard
      case "META-INF/versions/9/module-info.class" => MergeStrategy.discard
      case "google/protobuf/struct.proto" => MergeStrategy.first
      case x if x.contains("sigar") => MergeStrategy.first
      case x if x.contains("io.netty.versions.properties") => MergeStrategy.first
      case PathList("META-INF", xs @ _*) => xs.map(_.toLowerCase) match {
        case "manifest.mf" :: Nil | "index.list" :: Nil | "dependencies" :: Nil =>
          MergeStrategy.discard
        case _ => MergeStrategy.first
      }
      case "reference.conf" => MergeStrategy.concat
      case "application.conf" => MergeStrategy.concat
      case x if x.endsWith(".conf") => MergeStrategy.concat
      case x if x.endsWith(".properties") => MergeStrategy.first
      case _ => MergeStrategy.first
    }
  )