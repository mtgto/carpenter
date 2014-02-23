package net.mtgto.carpenter.infrastructure

import java.util.UUID
import play.api.db.slick.Config.driver.simple._

case class SourceRepository(projectId: String, software: String, url: String)

class SourceRepositories(tag: Tag) extends Table[SourceRepository](tag, "source_repositories") {
  def projectId = column[String]("project_id", O.NotNull)
  def software = column[String]("software", O.NotNull)
  def url = column[String]("url", O.NotNull)
  def * = (projectId, software, url) <> (SourceRepository.tupled, SourceRepository.unapply)
  def project = foreignKey("project_id", projectId, TableQuery[Projects])(_.id)
  def idx = index("project_id", projectId, unique = true)
}
