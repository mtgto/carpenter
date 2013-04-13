package net.mtgto.carpenter.infrastructure

import java.util.UUID
import play.api.db.DB
import anorm._
import java.net.URI

class DatabaseSourceRepositoryDao extends SourceRepositoryDao {
  import play.api.Play.current

  protected[this] def convertRowToSourceRepository(row: Row): SourceRepository = {
    row match {
      case Row(software: String, url: String) => SourceRepository(software, new URI(url))
    }
  }
  override def findByProjectId(id: UUID): Seq[SourceRepository] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `software`, `url` FROM `source_repositories` WHERE `project_id` = {projectId}")
      .on('projectId -> id.toString)().map(convertRowToSourceRepository).toList
    }
  }

  override def save(projectId: UUID, sourceRepository: SourceRepository): Unit = {
    DB.withConnection{ implicit c =>
      val rowCount =
        SQL("UPDATE `source_repositories` SET `software` = {software}, `url` = {url} WHERE `project_id` = {projectId}")
          .on('projectId -> projectId.toString, 'software -> sourceRepository.software, 'url -> sourceRepository.uri.toString)
          .executeUpdate()
      if (rowCount == 0)
        SQL("INSERT INTO `source_repositories` (`project_id`, `software`, `url`) VALUES ({projectId},{software},{url})")
          .on('projectId -> projectId.toString, 'software -> sourceRepository.software, 'url -> sourceRepository.uri.toString)
          .executeInsert()
    }
  }

  override def deleteByProjectId(projectId: UUID): Int = {
    DB.withConnection{ implicit c =>
      SQL("DELETE `source_repositories` WHERE `project_id` = {projectId}")
        .on('projectId -> projectId.toString).executeUpdate()
    }
  }
}
