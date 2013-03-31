package net.mtgto.domain

import java.util.UUID
import scalaz.Identity

trait JobFactory {
  def apply(project: Project, exitCode: Int, log: String): Job
}

object JobFactory extends JobFactory {
  override def apply(project: Project, exitCode: Int, log: String): Job = {
    Job(Identity(UUID.randomUUID), project: Project, exitCode: Int, log: String)
  }
}