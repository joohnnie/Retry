ThisBuild / organization         := "fun.zyx"
ThisBuild / organizationName     := "zyx"
ThisBuild / organizationHomepage := Some(url("https://github.com/joohnnie/Retry"))

ThisBuild / scmInfo := Some(
    ScmInfo(
        url("https://github.com/joohnnie/Retry"),
        "scm:git@github.com:joohnnie/Retry.git"
    )
)
ThisBuild / developers := List(
    Developer(
        id = "joohnnie",
        name = "Johnnie",
        email = "your@email",
        url = url("https://github.com/joohnnie/Retry")
    )
)

ThisBuild / description := "A lightweight and flexible Scala library for handling retries in synchronous and " +
  "asynchronous functions in case of failure"
ThisBuild / licenses := List(
    "Apache 2" -> new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")
)
ThisBuild / homepage := Some(url("https://github.com/joohnnie/Retry"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "content/repositories/releases")
}

ThisBuild / publishMavenStyle := true

ThisBuild / versionScheme := Some("early-semver")
