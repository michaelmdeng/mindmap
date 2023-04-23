Global / onChangedBuildSource := ReloadOnSourceChanges

val http4sVersion = "0.22.14"
val tofuVersion = "0.11.1"

lazy val commonSettings = Seq(
  name := "mindmap",
  organization := "com.michaelmdeng",
  scalaVersion := "2.13.10",
  crossPaths := false,
  scalacOptions ++= Seq("-feature", "-deprecation"),
  fork := true,
  libraryDependencies ++= Seq(
    "commons-io" % "commons-io" % "2.7",
    "org.apache.logging.log4j" % "log4j-core" % "2.17.1",
    "org.commonmark" % "commonmark" % "0.18.1",
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-ember-client" % http4sVersion,
    "org.http4s" %% "http4s-ember-server" % http4sVersion,
    "org.json4s" %% "json4s-native" % "3.7.0-M2",
    "org.scala-graph" %% "graph-core" % "1.13.2",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
    "org.typelevel" %% "cats-core" % "2.0.0",
    "org.typelevel" %% "cats-effect" % "2.1.2",
    "tf.tofu" %% "tofu-logging" % tofuVersion,
    "tf.tofu" %% "tofu-logging-derivation" % tofuVersion
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.0" % "test"
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  addCompilerPlugin(
    "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
  ),
  assembly / mainClass := Some("mindmap.Main"),
  assembly / assemblyJarName := "mindmap.jar"
)

lazy val root = (project in file("."))
  .settings(
    commonSettings
  )
  .enablePlugins(SbtTwirl)
