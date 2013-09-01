package net.mtgto.carpenter.infrastructure

import java.util.UUID
import play.api.db.slick.Config.driver.simple._
import org.sisioh.baseunits.scala.time.TimePoint
import net.mtgto.carpenter.infrastructure.slick.TimePointMapper._

case class Job(id: String, projectId: String, userId: String, task: String, exitCode: Option[Int],
               log: Option[String], executeTime: TimePoint, executeDuration: Option[Long])

object Jobs extends Table[Job]("jobs") {
  def id = column[String]("id", O.PrimaryKey)
  def projectId = column[String]("project_id", O.NotNull)
  def userId = column[String]("user_id", O.NotNull)
  def task = column[String]("task", O.NotNull)
  def exitCode = column[Option[Int]]("exit_code", O.Nullable)
  def log = column[Option[String]]("log", O.Nullable)
  def executeTime = column[TimePoint]("execute_time", O.NotNull)
  def executeDuration = column[Option[Long]]("execute_duration", O.Nullable)
  def * = id ~ projectId ~ userId ~ task ~ exitCode ~ log ~ executeTime ~ executeDuration <> (Job.apply _, Job.unapply _)
  def project = Projects.where(_.id === projectId)
}
