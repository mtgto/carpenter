package net.mtgto.carpenter.domain

import java.util.UUID
import org.sisioh.baseunits.scala.time.{Duration, TimePoint}
import scalaz.Identity

trait JobFactory {
  def apply(project: Project, user: User, exitCode: Int, log: String,
    executeTimePoint: TimePoint, executeDuration: Duration): Job
}

object JobFactory extends JobFactory {
  override def apply(project: Project, user: User, exitCode: Int, log: String,
    executeTimePoint: TimePoint, executeDuration: Duration): Job = {
    Job(Identity(UUID.randomUUID), project, user, exitCode, log, executeTimePoint, executeDuration)
  }
}