Global / onChangedBuildSource := ReloadOnSourceChanges

organization in ThisBuild := "com.michaelmdeng"
crossPaths in ThisBuild := false
scalacOptions in ThisBuild ++= Seq("-feature", "-deprecation")

val http4sVersion = "0.21.1"

lazy val commonSettings = Seq(
  name := "mindmap",
  scalaVersion := "2.13.1",
  libraryDependencies ++= Seq(
    "commons-io" % "commons-io" % "2.6",
    "org.http4s" %% "http4s-blaze-client" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,
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

lazy val jvm = (project in file("jvm"))
  .settings(
    commonSettings
  )
