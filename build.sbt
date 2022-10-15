ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "learning-scala"
  )

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1"
libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "2.0.0-beta1",
  "org.slf4j" % "slf4j-simple" % "2.0.0-beta1")
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.11" % Test