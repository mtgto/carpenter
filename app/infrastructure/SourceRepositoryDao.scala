package net.mtgto.carpenter.infrastructure

import java.util.UUID

trait SourceRepositoryDao {
  def findByProjectId(id: UUID): Seq[SourceRepository]

  def save(projectId: UUID, sourceRepository: SourceRepository): Unit
}
