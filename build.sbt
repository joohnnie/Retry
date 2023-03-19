name := "retry"

organization := "fun.zyx"

scalaVersion := "2.12.15"

crossScalaVersions := Seq("2.12.15", "2.13.10", "3.1.1")

version := "1.0.0"

libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.15" % Test
)

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xfatal-warnings")

ThisBuild / scalafmtOnCompile := true

organizationName := "zyx"
startYear        := Some(2023)
licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
