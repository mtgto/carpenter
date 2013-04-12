package net.mtgto.carpenter.infrastructure

import java.util.UUID
import play.api.db.DB
import anorm._
import java.net.URL

class DatabaseSourceRepositoryDao extends SourceRepositoryDao {
  protected[this] def convertRowToSourceRepository(row: Row): SourceRepository = {
    row match {
      case Row(software: String, url: String) => SourceRepository(software, new URL(url))
    }
  }
  override def findByProjectId(id: UUID): Seq[SourceRepository] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `software`, `url` FROM `source_repositories` WHERE `project_id` = {projectId}")
      .on('projectId -> id)().map(convertRowToSourceRepository).toList
    }
  }

  override def save(projectId: UUID, sourceRepository: SourceRepository): Unit = {
    DB.withConnection{ implicit c =>
      SQL("INSERT INTO `source_repositories` (`project_id`, `software`, `url`) VALUES ({projectId},{software},{url})")
      .on('projectId -> projectId, 'software -> sourceRepository.software, 'url -> sourceRepository.url.toString)()
    }
  }
}
