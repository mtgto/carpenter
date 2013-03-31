package net.mtgto.domain

import java.util.UUID
import org.sisioh.dddbase.core.Entity
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
  val exitCode: Option[Int]
  val log: String

  override def toString: String = Seq(identity, project).mkString("Job(", ", ",")")
}

object Job {
  private case class DefaultJob(identity: Identity[UUID], project: Project, exitCode: Option[Int], log: String) extends Job

  def apply(identity: Identity[UUID], project: Project, exitCode: Option[Int], log: String): Job = {
    DefaultJob(identity, project, exitCode, log)
  }
}
