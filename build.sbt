import play.Project._

name := "carpenter"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  "org.sisioh" %% "scala-dddbase-core" % "0.1.20",
  "org.sisioh" %% "baseunits-scala" % "0.1.6",
  "org.pircbotx" % "pircbotx" % "2.0.1",
  "com.typesafe.play" %% "play-slick" % "0.6.0.1"
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint", "-encoding", "UTF8")

resolvers ++= Seq("scala-dddbase Repository" at "http://sisioh.github.io/scala-dddbase/repos/release/",
  "baseunits-scala Repository" at "http://sisioh.github.io/baseunits-scala/repos/release/",
  "Sisioh Scala Toolbox Release Repository" at "http://sisioh.github.io/scala-toolbox/repos/release/"
)

playScalaSettings

templatesImport ++= Seq(
  "views.html.helper._",
  "net.mtgto.carpenter.controllers.Application.fieldConstructor",
  "play.api.i18n.Messages"
)

def customLessEntryPoints(base: File): PathFinder =
  (base / "app" / "assets" / "stylesheets" / "bootstrap" * "bootstrap.less") +++
    (base / "app" / "assets" / "stylesheets" / "bootstrap" * "responsive.less") +++
    (base / "app" / "assets" / "stylesheets" / "users" * "*.less") +++
    (base / "app" / "assets" / "stylesheets" * "*.less")

lessEntryPoints <<= baseDirectory(customLessEntryPoints)

