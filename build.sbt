Global / lintUnusedKeysOnLoad := false

scalaVersion     := "2.13.8"
name             := "workia"
organization     := "me.ooon"
target		     := studioTarget.value
idePackagePrefix := Some("me.ooon")


libraryDependencies ++= Seq(NSCALA, OS_LIB, SQUANTS, ORISON, TYPESAFE_CONFIG, PLAY_JSON)
libraryDependencies ++= Seq(SCALA_TEST, LOG).flatten

excludeDependencies ++= excludes
dependencyOverrides ++= overrides
