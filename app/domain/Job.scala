package net.mtgto.carpenter.domain

import java.util.UUID
import org.sisioh.dddbase.core.{Identity, Entity}
import org.sisioh.baseunits.scala.time.{Duration, TimePoint}
import net.mtgto.carpenter.domain.vcs.Snapshot

case class JobId(uuid: UUID) extends Identity[JobId] {
  override def value = this
}

/**
 * Job
 *
 * @param identity identity
 * @param project parent project
 * @param exitCode exit code
 * @param log log
 */
trait Job extends Entity[JobId] {
  override val identity: JobId
  val project: Project
  val user: User
  val snapshot: Snapshot
  val taskName: String
  val exitCode: Int
  val log: String
  val executeTimePoint: TimePoint
  val executeDuration: Duration

  override def toString: String = Seq(identity, project, user, snapshot, taskName, exitCode).mkString("Job(", ", ",")")
}

object Job {
  private case class DefaultJob(
    identity: JobId, project: Project, user: User, snapshot: Snapshot, taskName: String, exitCode: Int, log: String,
    executeTimePoint: TimePoint, executeDuration: Duration) extends Job

  def apply(identity: JobId, project: Project, user: User, snapshot: Snapshot, taskName: String, exitCode: Int, log: String,
            executeTimePoint: TimePoint, executeDuration: Duration): Job = {
    DefaultJob(identity, project, user, snapshot, taskName, exitCode, log, executeTimePoint, executeDuration)
  }
}
