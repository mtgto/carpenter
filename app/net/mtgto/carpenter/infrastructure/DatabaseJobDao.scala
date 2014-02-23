package net.mtgto.carpenter.infrastructure

import java.sql.{Clob, Timestamp}
import java.util.{Date, UUID}
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

class DatabaseJobDao extends JobDao {
  import play.api.Play.current

  private[this] val jobs = TableQuery[Jobs]

  private[this] val projects = TableQuery[Projects]

  private[this] val users = TableQuery[Users]

  private[this] val authorities = TableQuery[Authorities]

//  protected[this] def convertRowToJob(row: Row): Job = {
//    row match {
//      case Row(id: String, task: String, Some(exitCode: Int), Some(log: Clob), executeTime: Timestamp, Some(executeDuration: Int),
//               userId: String, userName: String, userCanLogin: Byte, userCanCreateUser: Byte,
//               projectId: String, projectName: String, projectHostname: String, projectRecipe: Clob) =>
//        Job(UUID.fromString(id),
//            Project(UUID.fromString(projectId), projectName, projectHostname, projectRecipe.getSubString(1, projectRecipe.length.toInt)),
//            User(UUID.fromString(userId), userName, Authority(userCanLogin != 0, userCanCreateUser != 0)),
//            task, Some(exitCode),
//            Some(log.getSubString(1, log.length.toInt)), new Date(executeTime.getTime), Some(executeDuration.toLong))
//      case Row(id: String, task: String, None, None, executeTime: Timestamp, None,
//               userId: String, userName: String, userCanLogin: Byte, userCanCreateUser: Byte,
//               projectId: String, projectName: String, projectHostname: String, projectRecipe: Clob) =>
//        Job(UUID.fromString(id),
//          Project(UUID.fromString(projectId), projectName, projectHostname, projectRecipe.getSubString(1, projectRecipe.length.toInt)),
//          User(UUID.fromString(userId), userName, Authority(userCanLogin != 0, userCanCreateUser != 0)),
//          task, None, None, new Date(executeTime.getTime), None)
//    }
//  }

  override def findById(id: String): Option[(Job, Project, (User, Authority))] = {
    DB.withSession { implicit session =>
      val query = for {
        job <- jobs if job.id === id
        project <- projects if project.id === job.projectId
        user <- users if user.id === job.userId
        authority <- authorities if authority.userId === user.id
      } yield (job, project, (user, authority))
      query.firstOption
//    DB.withConnection{ implicit c =>
//      SQL(
//        """
//          | SELECT `jobs`.`id`, `jobs`.`task`, `jobs`.`exit_code`, `jobs`.`log`, `jobs`.`execute_time`, `jobs`.`execute_duration`,
//          | `users`.`id` AS `user_id`, `users`.`name` AS `user_name`, `authorities`.`can_login` AS `user_can_login`, `authorities`.`can_create_user` AS `user_can_create_user`,
//          | `projects`.`id` AS `project_id`, `projects`.`name` AS `project_name`, `projects`.`hostname` AS `project_hostname`, `projects`.`recipe` AS `project_recipe`
//          | FROM `jobs`
//          |   INNER JOIN `users` ON `jobs`.`user_id` = `users`.`id`
//          |   INNER JOIN `projects` ON `jobs`.`project_id` = `projects`.`id`
//          |   INNER JOIN `authorities` ON `users`.`id` = `authorities`.`user_id`
//          | WHERE `jobs`.`id` = {id}""".stripMargin)
//      .on('id -> id.toString)()
//      .headOption.map(convertRowToJob)
    }
  }

  override def findAll: Seq[(Job, Project, (User, Authority))] = {
    DB.withSession { implicit session =>
      val query = for {
        job <- jobs
        project <- projects if project.id === job.projectId
        user <- users if user.id === job.userId
        authority <- authorities if authority.userId === user.id
      } yield (job, project, (user, authority))
      query.list
    }
//    DB.withConnection{ implicit c =>
//      SQL(
//        """
//          | SELECT `jobs`.`id`, `jobs`.`task`, `jobs`.`exit_code`, `jobs`.`log`, `jobs`.`execute_time`, `jobs`.`execute_duration`,
//          | `users`.`id` AS `user_id`, `users`.`name` AS `user_name`, `authorities`.`can_login` AS `user_can_login`, `authorities`.`can_create_user` AS `user_can_create_user`,
//          | `projects`.`id` AS `project_id`, `projects`.`name` AS `project_name`, `projects`.`hostname` AS `project_hostname`, `projects`.`recipe` AS `project_recipe`
//          | FROM `jobs`
//          |   INNER JOIN `users` ON `jobs`.`user_id` = `users`.`id`
//          |   INNER JOIN `projects` ON `jobs`.`project_id` = `projects`.`id`
//          |   INNER JOIN `authorities` ON `users`.`id` = `authorities`.`user_id`""".stripMargin)()
//      .map(convertRowToJob).toList
//    }
  }

  override def findAllByProject(projectId: String): Seq[(Job, Project, (User, Authority))] = {
    DB.withSession { implicit session =>
      val query = for {
        job <- jobs if job.projectId === projectId
        project <- projects if project.id === job.projectId
        user <- users if user.id === job.userId
        authority <- authorities if authority.userId === user.id
      } yield (job, project, (user, authority))
      query.list
    }
//    DB.withConnection{ implicit c =>
//      SQL(
//        """
//          | SELECT `jobs`.`id`, `jobs`.`task`, `jobs`.`exit_code`, `jobs`.`log`, `jobs`.`execute_time`, `jobs`.`execute_duration`,
//          | `users`.`id` AS `user_id`, `users`.`name` AS `user_name`, `authorities`.`can_login` AS `user_can_login`, `authorities`.`can_create_user` AS `user_can_create_user`,
//          | `projects`.`id` AS `project_id`, `projects`.`name` AS `project_name`, `projects`.`hostname` AS `project_hostname`, `projects`.`recipe` AS `project_recipe`
//          | FROM `jobs`
//          |   INNER JOIN `users` ON `jobs`.`user_id` = `users`.`id`
//          |   INNER JOIN `projects` ON `jobs`.`project_id` = `projects`.`id`
//          |   INNER JOIN `authorities` ON `users`.`id` = `authorities`.`user_id`
//          | WHERE `jobs`.`project_id` = {projectId}""".stripMargin
//      )
//      .on('projectId -> projectId.toString)()
//      .map(convertRowToJob).toList
//    }
  }

  override def findAllByProjectOrderByDateDesc(projectId: String): Seq[(Job, Project, (User, Authority))] = {
    DB.withSession { implicit session =>
      val query = for {
        job <- jobs.sortBy(_.executeTime.desc) if job.projectId === projectId
        project <- projects if project.id === job.projectId
        user <- users if user.id === job.userId
        authority <- authorities if authority.userId === user.id
      } yield (job, project, (user, authority))
      query.list
    }
//    DB.withConnection{ implicit c =>
//      SQL(
//        """
//          | SELECT `jobs`.`id`, `jobs`.`task`, `jobs`.`exit_code`, `jobs`.`log`, `jobs`.`execute_time`, `jobs`.`execute_duration`,
//          | `users`.`id` AS `user_id`, `users`.`name` AS `user_name`, `authorities`.`can_login` AS `user_can_login`, `authorities`.`can_create_user` AS `user_can_create_user`,
//          | `projects`.`id` AS `project_id`, `projects`.`name` AS `project_name`, `projects`.`hostname` AS `project_hostname`, `projects`.`recipe` AS `project_recipe`
//          | FROM `jobs`
//          |   INNER JOIN `users` ON `jobs`.`user_id` = `users`.`id`
//          |   INNER JOIN `projects` ON `jobs`.`project_id` = `projects`.`id`
//          |   INNER JOIN `authorities` ON `users`.`id` = `authorities`.`user_id`
//          | WHERE `jobs`.`project_id` = {projectId}
//          | ORDER BY `execute_time` DESC""".stripMargin
//      )
//      .on('projectId -> projectId.toString)()
//      .map(convertRowToJob).toList
//    }
  }

  override def findAllByUser(userId: String): Seq[(Job, Project, (User, Authority))] = {
    DB.withSession { implicit session =>
      val query = for {
        job <- jobs if job.userId === userId
        project <- projects if project.id === job.projectId
        user <- users if user.id === userId
        authority <- authorities if authority.userId === userId
      } yield (job, project, (user, authority))
      query.list
    }
//    DB.withConnection{ implicit c =>
//      SQL(
//        """
//          | SELECT `jobs`.`id`, `jobs`.`task`, `jobs`.`exit_code`, `jobs`.`log`, `jobs`.`execute_time`, `jobs`.`execute_duration`,
//          | `users`.`id` AS `user_id`, `users`.`name` AS `user_name`, `authorities`.`can_login` AS `user_can_login`, `authorities`.`can_create_user` AS `user_can_create_user`,
//          | `projects`.`id` AS `project_id`, `projects`.`name` AS `project_name`, `projects`.`hostname` AS `project_hostname`, `projects`.`recipe` AS `project_recipe`
//          | FROM `jobs`
//          |   INNER JOIN `users` ON `jobs`.`user_id` = `users`.`id`
//          |   INNER JOIN `projects` ON `jobs`.`project_id` = `projects`.`id`
//          |   INNER JOIN `authorities` ON `users`.`id` = `authorities`.`user_id`
//          | WHERE `jobs`.`user_id` = {userId}""".stripMargin
//      )
//        .on('userId -> userId.toString)()
//        .map(convertRowToJob).toList
//    }
  }

  override def save(job: Job) {
    DB.withTransaction { implicit session: Session =>
      val rowCount = jobs.where(_.id === job.id)
        .map(j => (j.projectId, j.userId, j.task, j.exitCode, j.log, j.executeTime, j.executeDuration))
        .update((job.projectId, job.userId, job.task, job.exitCode, job.log, job.executeTime, job.executeDuration))
      if (rowCount == 0) {
        jobs += job
      }
    }
//    DB.withConnection{ implicit c =>
//      val rowCount =
//        SQL("""UPDATE `jobs` SET `project_id` = {projectId}, `user_id` = {userId}, `task` = {task}, `exit_code` = {exitCode},
//              |`log` = {log}, `execute_time` = {executeTime}, `execute_duration` = {executeDuration} WHERE `id` = {id}""".stripMargin)
//          .on('id -> job.id.toString, 'projectId -> job.project.id.toString, 'userId -> job.user.id.toString, 'task -> job.task,
//              'exitCode -> job.exitCode.map(_.toString), 'log -> job.log, 'executeTime -> job.executeDate, 'executeDuration -> job.executeDuration).executeUpdate()
//      if (rowCount == 0)
//        SQL("""INSERT INTO `jobs` (`id`, `project_id`, `user_id`, `task`, `exit_code`, `log`, `execute_time`, `execute_duration`)
//              |VALUES ({id},{projectId},{userId},{task},{exitCode},{log},{executeTime},{executeDuration})""".stripMargin)
//          .on('id -> job.id.toString, 'projectId -> job.project.id.toString, 'userId -> job.user.id.toString,
//              'task -> job.task, 'exitCode -> job.exitCode.map(_.toString), 'log -> job.log,
//              'executeTime -> job.executeDate, 'executeDuration -> job.executeDuration).executeInsert()
//    }
  }

  override def delete(id: String): Int = {
    DB.withSession { implicit session =>
      jobs.where(_.id === id).delete
    }
//    DB.withConnection{ implicit c =>
//      SQL("DELETE `jobs` WHERE `id` = {id}")
//        .on('id -> id.toString).executeUpdate()
//    }
  }
}
