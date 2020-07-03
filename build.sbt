Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val commonSettings = Seq(
  name := "mindmap",
  organization := "com.michaelmdeng",
  scalaVersion := "2.13.1",
  crossPaths := false,
  scalacOptions ++= Seq("-feature", "-deprecation"),
  libraryDependencies ++= Seq(
    "commons-io" % "commons-io" % "2.6",
    "log4j" % "log4j" % "1.2.17",
    "org.json4s" %% "json4s-native" % "3.7.0-M2",
    "org.scala-graph" %% "graph-core" % "1.13.2",
    "org.typelevel" %% "cats-core" % "2.0.0",
    "org.typelevel" %% "cats-effect" % "2.1.2"
  ),
  addCompilerPlugin(
    "org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full
  )
)

lazy val root = (project in file("."))
  .settings(
    commonSettings
  )
