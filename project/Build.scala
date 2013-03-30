import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "carpenter"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm
  )

  // Only compile the bootstrap bootstrap.less file and any other *.less file in the stylesheets directory
  def customLessEntryPoints(base: File): PathFinder = (
    (base / "app" / "assets" / "stylesheets" / "bootstrap" * "bootstrap.less") +++
    (base / "app" / "assets" / "stylesheets" * "*.less")
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint", "-encoding", "UTF8"),
    resolvers += "scala-dddbase Repository" at "http://sisioh.github.com/scala-dddbase/repos/release/",
    lessEntryPoints <<= baseDirectory(customLessEntryPoints)
  )

}
