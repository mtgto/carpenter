package net.mtgto.infrastructure

import java.util.{Date, UUID}

case class Job(id: UUID, projectId: UUID, userId: UUID, exitCode: Int, log: String,
  executeDate: Date, executeDuration: Long)
