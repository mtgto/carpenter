package net.mtgto.carpenter.infrastructure

import anorm._
import anorm.SqlParser._
import java.sql.{Clob, Timestamp}
import java.util.UUID
import java.util.Date
import play.api.db.DB

class DatabaseJobDao extends JobDao {
  import play.api.Play.current

  protected[this] def convertRowToJob(row: Row): Job = {
    row match {
      case Row(id: String, projectId: String, userId: String, exitCode: Int, log: Clob, executeTime: Timestamp, executeDuration: Int) =>
        Job(UUID.fromString(id), UUID.fromString(projectId), UUID.fromString(userId), exitCode,
            log.getSubString(1, log.length.toInt), new Date(executeTime.getTime), executeDuration.toLong)
    }
  }

  override def findById(id: UUID): Option[Job] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `project_id`, `user_id`, `exit_code`, `log`, `execute_time`, `execute_duration` FROM `jobs` WHERE `id` = {id}")
      .on('id -> id.toString)()
      .headOption.map(convertRowToJob)
    }
  }

  override def findAll: Seq[Job] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `project_id`, `user_id`, `exit_code`, `log`, `execute_time`, `execute_duration` FROM `jobs`")()
      .map(convertRowToJob).toList
    }
  }

  override def findAllByProject(projectId: UUID): Seq[Job] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `project_id`, `user_id`, `exit_code`, `log`, `execute_time`, `execute_duration` FROM `jobs` WHERE `project_id` = {projectId}")
      .on('projectId -> projectId.toString)()
      .map(convertRowToJob).toList
    }
  }

  override def findAllByProjectOrderByDateDesc(projectId: UUID): Seq[Job] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `project_id`, `user_id`, `exit_code`, `log`, `execute_time`, `execute_duration` FROM `jobs` WHERE `project_id` = {projectId} ORDER BY `execute_time` DESC")
      .on('projectId -> projectId.toString)()
      .map(convertRowToJob).toList
    }
  }

  override def save(id: UUID, projectId: UUID, userId: UUID, exitCode: Int, log: String, executeDate: Date, executeDuration: Long): Unit = {
    DB.withConnection{ implicit c =>
      val rowCount =
        SQL("""UPDATE `jobs` SET `project_id` = {projectId}, `user_id` = {userId}, `exit_code` = {exitCode},
              |`log` = {log}, `execute_time` = {executeTime}, `execute_duration` = {executeDuration} WHERE `id` = {id}""".stripMargin)
          .on('id -> id.toString, 'projectId -> projectId.toString, 'userId -> userId.toString, 'exitCode -> exitCode, 'log -> log,
              'executeTime -> executeDate, 'executeDuration -> executeDuration).executeUpdate()
      if (rowCount == 0)
        SQL("""INSERT INTO `jobs` (`id`, `project_id`, `user_id`, `exit_code`, `log`, `execute_time`, `execute_duration`)
              |VALUES ({id},{projectId},{userId},{exitCode},{log},{executeTime},{executeDuration})""".stripMargin)
          .on('id -> id.toString, 'projectId -> projectId.toString, 'userId -> userId.toString, 'exitCode -> exitCode, 'log -> log,
              'executeTime -> executeDate, 'executeDuration -> executeDuration).executeInsert()
    }
  }

  override def delete(id: UUID): Int = {
    DB.withConnection{ implicit c =>
      SQL("DELETE `jobs` WHERE `id` = {id}")
        .on('id -> id.toString).executeUpdate()
    }
  }
}
