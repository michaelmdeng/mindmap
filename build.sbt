Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val commonSettings = Seq(
  name := "mindmap",
  organization := "com.michaelmdeng",
  scalaVersion := "2.13.10",
  crossPaths := false,
  scalacOptions ++= Seq("-feature", "-deprecation"),
  libraryDependencies ++= Seq(
    "commons-io" % "commons-io" % "2.7",
    "org.apache.logging.log4j" % "log4j-core" % "2.17.1",
    "org.commonmark" % "commonmark" % "0.18.1",
    "org.json4s" %% "json4s-native" % "3.7.0-M2",
    "org.scala-graph" %% "graph-core" % "1.13.2",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
    "org.typelevel" %% "cats-core" % "2.0.0",
    "org.typelevel" %% "cats-effect" % "2.1.2"
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.0" % "test"
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  addCompilerPlugin(
    "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
  )
)

lazy val root = (project in file("."))
  .settings(
    commonSettings
  )
