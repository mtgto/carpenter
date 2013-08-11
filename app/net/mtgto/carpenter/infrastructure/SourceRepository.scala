package net.mtgto.carpenter.infrastructure

import java.util.UUID
import play.api.db.slick.Config.driver.simple._

case class SourceRepository(projectId: String, software: String, url: String)

object SourceRepositories extends Table[SourceRepository]("source_repositories") {
  def projectId = column[String]("project_id", O.NotNull)
  def software = column[String]("software", O.NotNull)
  def url = column[String]("url", O.NotNull)
  def * = projectId ~ software ~ url <> (SourceRepository.apply _, SourceRepository.unapply _)
  def project = foreignKey("project_id", projectId, Projects)(_.id)
  def idx = index("project_id", projectId, unique = true)
}
