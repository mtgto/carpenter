import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "carpenter"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    jdbc,
    anorm,
    "org.sisioh" %% "scala-dddbase-core" % "0.1.17",
    "org.sisioh" %% "baseunits-scala" % "0.1.4",
    "org.pircbotx" % "pircbotx" % "1.9"
  )

  // Only compile the bootstrap bootstrap.less file and any other *.less file in the stylesheets directory
  def customLessEntryPoints(base: File): PathFinder = (
    (base / "app" / "assets" / "stylesheets" / "bootstrap" * "bootstrap.less") +++
    (base / "app" / "assets" / "stylesheets" / "bootstrap" * "responsive.less") +++
    (base / "app" / "assets" / "stylesheets" / "users" * "*.less") +++
    (base / "app" / "assets" / "stylesheets" * "*.less")
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint", "-encoding", "UTF8"),
    resolvers ++= Seq("scala-dddbase Repository" at "http://sisioh.github.io/scala-dddbase/repos/release/",
                      "baseunits-scala Repository" at "http://sisioh.github.io/baseunits-scala/repos/release/",
                      "Sisioh Scala Toolbox Release Repository" at "http://sisioh.github.io/scala-toolbox/repos/release/"),
    templatesImport ++= Seq("views.html.helper._", "net.mtgto.carpenter.controllers.Application.fieldConstructor", "play.api.i18n.Messages"),
    lessEntryPoints <<= baseDirectory(customLessEntryPoints)
  )

}
