version          := "0.1.0"
scalaVersion     := "2.13.10"
name             := "workia"
organization     := "me.ooon"

Global / excludeLintKeys := Set(idePackagePrefix)

idePackagePrefix := Some("me.ooon.workia")


libraryDependencies ++= Seq(NSCALA, OS_LIB, SQUANTS, ORISON, TYPESAFE_CONFIG, PLAY_JSON)
libraryDependencies ++= Seq(SCALA_TEST, LOG).flatten

excludeDependencies ++= Seq(
  ExclusionRule("org.slf4j", "slf4j-log4j12"),
  ExclusionRule("log4j", "log4j")
)
