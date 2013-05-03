package net.mtgto.carpenter.domain

import java.util.UUID
import net.mtgto.carpenter.domain.vcs.Snapshot
import org.sisioh.baseunits.scala.time.{Duration, TimePoint}

trait JobFactory {
  def apply(project: Project, user: User, snapshot: Snapshot, taskName: String,
            executeTimePoint: TimePoint): Job

  def apply(job: Job, exitCode: Int, log: String, executeDuration: Duration): Job
}

object JobFactory extends JobFactory {
  override def apply(project: Project, user: User, snapshot: Snapshot, taskName: String,
                     executeTimePoint: TimePoint): Job = {
    Job(JobId(UUID.randomUUID), project, user, snapshot, taskName, executeTimePoint)
  }

  override def apply(job: Job, exitCode: Int, log: String, executeDuration: Duration): Job = {
    Job(job.identity, job.project, job.user, job.snapshot,  job.taskName, exitCode,
      log, job.executeTimePoint, executeDuration)
  }
}
