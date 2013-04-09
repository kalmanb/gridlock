import sbt._
import Keys._
import sbt.Defaults._

object LockManager extends Build {
  lazy val projectName = "akka-lockmanager"
  lazy val ProjectVersion = "0.2.0-SNAPSHOT"

  lazy val akkaVersion = "2.1.2"

  lazy val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  lazy val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  lazy val akkaRemoting = "com.typesafe.akka" %% "akka-remoting" % akkaVersion
  lazy val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion

  lazy val slf4j = "org.slf4j" % "slf4j-api" % "1.7.2"
// lazy val scalaTest = "org.scalatest" %% "scalatest" % "2.0.M6-SNAP12" % "test"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "2.0.M5b" % "test"
  lazy val junit = "junit" % "junit" % "4.11" % "test"
  lazy val mockito = "org.mockito" % "mockito-all" % "1.9.5" % "test"

  lazy val root = Project(
    id = projectName,
    base = file("."),
    settings = defaultSettings ++ Seq(
      scalaVersion := "2.10.1",
      organization := "org.kalmanb",
      version := ProjectVersion,
      libraryDependencies ++= Seq(
        akkaActor,
        akkaSlf4j,
        akkaTestKit,
        slf4j,
        scalaTest,
        junit,
        mockito
      )
    )
  )
}

