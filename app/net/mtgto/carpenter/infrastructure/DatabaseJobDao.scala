package net.mtgto.carpenter.infrastructure

import anorm._
import java.sql.{Clob, Timestamp}
import java.util.{Date, UUID}
import play.api.db.DB

class DatabaseJobDao extends JobDao {
  import play.api.Play.current

  protected[this] def convertRowToJob(row: Row): Job = {
    row match {
      case Row(id: String, projectId: String, userId: String, task: String, Some(exitCode: Int), Some(log: Clob), executeTime: Timestamp, Some(executeDuration: Int)) =>
        Job(UUID.fromString(id), UUID.fromString(projectId), UUID.fromString(userId), task, Some(exitCode),
            Some(log.getSubString(1, log.length.toInt)), new Date(executeTime.getTime), Some(executeDuration.toLong))
      case Row(id: String, projectId: String, userId: String, task: String, None, None, executeTime: Timestamp, None) =>
        Job(UUID.fromString(id), UUID.fromString(projectId), UUID.fromString(userId), task, None,
          None, new Date(executeTime.getTime), None)
    }
  }

  override def findById(id: UUID): Option[Job] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `project_id`, `user_id`, `task`, `exit_code`, `log`, `execute_time`, `execute_duration` FROM `jobs` WHERE `id` = {id}")
      .on('id -> id.toString)()
      .headOption.map(convertRowToJob)
    }
  }

  override def findAll: Seq[Job] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `project_id`, `user_id`, `task`, `exit_code`, `log`, `execute_time`, `execute_duration` FROM `jobs`")()
      .map(convertRowToJob).toList
    }
  }

  override def findAllByProject(projectId: UUID): Seq[Job] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `project_id`, `user_id`, `task`, `exit_code`, `log`, `execute_time`, `execute_duration` FROM `jobs` WHERE `project_id` = {projectId}")
      .on('projectId -> projectId.toString)()
      .map(convertRowToJob).toList
    }
  }

  override def findAllByProjectOrderByDateDesc(projectId: UUID): Seq[Job] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `project_id`, `user_id`, `task`, `exit_code`, `log`, `execute_time`, `execute_duration` FROM `jobs` WHERE `project_id` = {projectId} ORDER BY `execute_time` DESC")
      .on('projectId -> projectId.toString)()
      .map(convertRowToJob).toList
    }
  }

  override def findAllByUser(userId: UUID): Seq[Job] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `project_id`, `user_id`, `task`, `exit_code`, `log`, `execute_time`, `execute_duration` FROM `jobs` WHERE `user_id` = {userId}")
        .on('userId -> userId.toString)()
        .map(convertRowToJob).toList
    }
  }

  override def save(job: Job): Unit = {
    DB.withConnection{ implicit c =>
      val rowCount =
        SQL("""UPDATE `jobs` SET `project_id` = {projectId}, `user_id` = {userId}, `task` = {task}, `exit_code` = {exitCode},
              |`log` = {log}, `execute_time` = {executeTime}, `execute_duration` = {executeDuration} WHERE `id` = {id}""".stripMargin)
          .on('id -> job.id.toString, 'projectId -> job.projectId.toString, 'userId -> job.userId.toString, 'task -> job.task,
              'exitCode -> job.exitCode.map(_.toString), 'log -> job.log, 'executeTime -> job.executeDate, 'executeDuration -> job.executeDuration).executeUpdate()
      if (rowCount == 0)
        SQL("""INSERT INTO `jobs` (`id`, `project_id`, `user_id`, `task`, `exit_code`, `log`, `execute_time`, `execute_duration`)
              |VALUES ({id},{projectId},{userId},{task},{exitCode},{log},{executeTime},{executeDuration})""".stripMargin)
          .on('id -> job.id.toString, 'projectId -> job.projectId.toString, 'userId -> job.userId.toString,
              'task -> job.task, 'exitCode -> job.exitCode.map(_.toString), 'log -> job.log,
              'executeTime -> job.executeDate, 'executeDuration -> job.executeDuration).executeInsert()
    }
  }

  override def delete(id: UUID): Int = {
    DB.withConnection{ implicit c =>
      SQL("DELETE `jobs` WHERE `id` = {id}")
        .on('id -> id.toString).executeUpdate()
    }
  }
}
