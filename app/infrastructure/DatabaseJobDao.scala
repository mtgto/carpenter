package net.mtgto.infrastructure

import anorm._
import anorm.SqlParser._
import play.api.db.DB

import java.util.UUID

class DatabaseJobDao extends JobDao {
  import play.api.Play.current

  protected[this] def convertRowToJob(row: Row): Job = {
    row match {
      case Row(id: String, projectId: String, exitCode: Int, log: java.sql.Clob) =>
        Job(UUID.fromString(id), UUID.fromString(projectId), exitCode, log.getSubString(1, log.length.toInt))
    }
  }

  override def findById(id: UUID): Option[Job] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `project_id`, `exit_code`, `log` FROM `jobs` WHERE `id` = {id}").on("id" -> id)()
      .headOption.map(convertRowToJob)
    }
  }

  override def findAll: Seq[Job] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `project_id`, `exit_code`, `log` FROM `jobs`")().map(convertRowToJob).toList
    }
  }

  override def save(id: UUID, projectId: UUID, exitCode: Int, log: String): Unit = {
    DB.withConnection{ implicit c =>
      val rowCount =
        SQL("UPDATE `jobs` SET `project_id` = {projectId}, `exit_code` = {exitCode}, `log` = {log} WHERE `id` = {id}")
          .on('id -> id, 'projectId -> projectId, 'exitCode -> exitCode, 'log -> log).executeUpdate()
      if (rowCount == 0)
        SQL("INSERT INTO `jobs` (`id`, `project_id`, `exit_code`, `log`) VALUES ({id},{projectId},{exitCode},{log})")
          .on('id -> id, 'projectId -> projectId, 'exitCode -> exitCode, 'log -> log).executeInsert()
    }
  }

  override def delete(id: UUID): Int = {
    DB.withConnection{ implicit c =>
      SQL("DELETE `jobs` WHERE `id` = {id}")
        .on('id -> id).executeUpdate()
    }
  }
}
