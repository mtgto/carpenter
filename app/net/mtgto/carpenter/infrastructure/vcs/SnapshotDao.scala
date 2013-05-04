package net.mtgto.carpenter.infrastructure.vcs

import java.util.UUID

trait SnapshotDao {
  def findByJobId(id: UUID): Option[Snapshot]
  def save(jobId: UUID, name: String, revision: String, branchType: String): Unit
}
