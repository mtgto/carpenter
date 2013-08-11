package net.mtgto.carpenter.infrastructure.vcs

import java.util.UUID

trait SnapshotDao {
  def findByJobId(id: String): Option[Snapshot]
  def save(jobId: String, name: String, revision: String, branchType: String): Unit
}
