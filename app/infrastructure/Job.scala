package net.mtgto.infrastructure

import java.util.UUID

case class Job(id: UUID, projectId: UUID, exitCode: Int, log: String)
