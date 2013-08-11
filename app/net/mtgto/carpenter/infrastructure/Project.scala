package net.mtgto.carpenter.infrastructure

import java.util.UUID
import play.api.db.slick.Config.driver.simple._

case class Project(id: String, name: String, hostname: String, recipe: String)

object Projects extends Table[Project]("projects") {
  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name", O.NotNull)
  def hostname = column[String]("hostname", O.NotNull)
  def recipe = column[String]("recipe", O.NotNull)
  def * = id ~ name ~ hostname ~ recipe <> (Project, Project.unapply _)
}
