package net.mtgto.carpenter.domain

import java.util.UUID
import org.sisioh.dddbase.core.Entity
import org.sisioh.baseunits.scala.time.{Duration, TimePoint}
import scalaz.Identity

/**
 * Job
 *
 * @param identity identity
 * @param project parent project
 * @param exitCode exit code
 * @param log log
 */
trait Job extends Entity[UUID] {
  override val identity: Identity[UUID]
  val project: Project
  val user: User
  val exitCode: Int
  val log: String
  val executeTimePoint: TimePoint
  val executeDuration: Duration

  override def toString: String = Seq(identity, project).mkString("Job(", ", ",")")
}

object Job {
  private case class DefaultJob(
    identity: Identity[UUID], project: Project, user: User, exitCode: Int, log: String, executeTimePoint: TimePoint,
    executeDuration: Duration) extends Job

  def apply(identity: Identity[UUID], project: Project, user: User, exitCode: Int, log: String, executeTimePoint: TimePoint,
    executeDuration: Duration): Job = {
    DefaultJob(identity, project, user, exitCode, log, executeTimePoint, executeDuration)
  }
}
