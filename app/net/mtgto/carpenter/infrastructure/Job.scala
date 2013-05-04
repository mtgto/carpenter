package net.mtgto.carpenter.infrastructure

import java.util.{Date, UUID}

case class Job(id: UUID, projectId: UUID, userId: UUID, task: String, exitCode: Option[Int],
               log: Option[String], executeDate: Date, executeDuration: Option[Long])
