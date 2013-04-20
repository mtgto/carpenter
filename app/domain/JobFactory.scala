package net.mtgto.carpenter.domain

import java.util.UUID
import org.sisioh.baseunits.scala.time.{Duration, TimePoint}
import net.mtgto.carpenter.domain.vcs.Snapshot

trait JobFactory {
  def apply(project: Project, user: User, snapshot: Snapshot, exitCode: Int, log: String,
    executeTimePoint: TimePoint, executeDuration: Duration): Job
}

object JobFactory extends JobFactory {
  override def apply(project: Project, user: User, snapshot: Snapshot, exitCode: Int, log: String,
    executeTimePoint: TimePoint, executeDuration: Duration): Job = {
    Job(JobId(UUID.randomUUID), project, user, snapshot, exitCode, log, executeTimePoint, executeDuration)
  }
}