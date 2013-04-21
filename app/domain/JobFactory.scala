package net.mtgto.carpenter.domain

import java.util.UUID
import org.sisioh.baseunits.scala.time.{Duration, TimePoint}
import net.mtgto.carpenter.domain.vcs.Snapshot

trait JobFactory {
  def apply(project: Project, user: User, snapshot: Snapshot, taskName: String, exitCode: Int, log: String,
    executeTimePoint: TimePoint, executeDuration: Duration): Job
}

object JobFactory extends JobFactory {
  override def apply(project: Project, user: User, snapshot: Snapshot, taskName: String, exitCode: Int, log: String,
    executeTimePoint: TimePoint, executeDuration: Duration): Job = {
    Job(JobId(UUID.randomUUID), project, user, snapshot, taskName, exitCode, log, executeTimePoint, executeDuration)
  }
}