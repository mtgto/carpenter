package net.mtgto.carpenter.domain

import java.util.UUID
import net.mtgto.carpenter.domain.vcs.Snapshot
import org.sisioh.dddbase.core.model.{Identity, Entity}
import org.sisioh.baseunits.scala.time.{Duration, TimePoint}

case class JobId(uuid: UUID) extends Identity[UUID] {
  override def value = uuid
}

/**
 * Job
 *
 * @param identity identity
 * @param project parent project
 * @param user user who execute this job
 * @param snapshot source repository snapshot to deploy
 * @param taskName name of the task
 * @param exitCode exit code if job has been completed
 * @param log log if job is running or completed
 * @param executeTimePoint starting time point to execute
 * @param executeDuration job executing duration if job has been completed
 */
trait Job extends Entity[JobId] {
  override val identity: JobId
  val project: Project
  val user: User
  val snapshot: Snapshot
  val taskName: String
  val exitCode: Option[Int]
  val log: Option[String]
  val executeTimePoint: TimePoint
  val executeDuration: Option[Duration]

  def isSuccess: Boolean = exitCode == Some(0)

  def isRunning: Boolean = exitCode.isEmpty

  override def toString: String = Seq(identity, project, user, snapshot, taskName, exitCode).mkString("Job(", ", ",")")
}

object Job {
  private case class DefaultJob(
    identity: JobId, project: Project, user: User, snapshot: Snapshot, taskName: String, exitCode: Option[Int],
    log: Option[String], executeTimePoint: TimePoint, executeDuration: Option[Duration]) extends Job

  def apply(identity: JobId, project: Project, user: User, snapshot: Snapshot, taskName: String, exitCode: Int, log: String,
            executeTimePoint: TimePoint, executeDuration: Duration): Job = {
    DefaultJob(identity, project, user, snapshot, taskName, Some(exitCode), Some(log), executeTimePoint, Some(executeDuration))
  }

  def apply(identity: JobId, project: Project, user: User, snapshot: Snapshot, taskName: String,
            executeTimePoint: TimePoint): Job = {
    DefaultJob(identity, project, user, snapshot, taskName, None, None, executeTimePoint, None)
  }
}
