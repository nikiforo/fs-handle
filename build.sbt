ThisBuild / scalaVersion := "2.13.4"
ThisBuild / organization := "name.nikiforo"

lazy val root = (project in file("."))
  .settings(
    name := "fs2handle",
    version := "0.0.1",
    addCompilerPlugin("com.olegpy" % "better-monadic-for_2.13" % "0.3.1"),
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "2.4.6",
      "co.fs2" %% "fs2-io" % "2.4.6",
      "org.scodec" %% "scodec-core" % "1.11.7",
      "org.scodec" %% "scodec-stream" % "2.0.0",
    )
  )
