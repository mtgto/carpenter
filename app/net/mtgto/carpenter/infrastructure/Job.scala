package net.mtgto.carpenter.infrastructure

import java.util.{Date, UUID}

case class Job(id: UUID, project: Project, user: User, task: String, exitCode: Option[Int],
               log: Option[String], executeDate: Date, executeDuration: Option[Long])
