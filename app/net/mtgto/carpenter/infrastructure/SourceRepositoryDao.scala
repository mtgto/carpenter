package net.mtgto.carpenter.infrastructure

import java.util.UUID

trait SourceRepositoryDao {
  def findByProjectId(id: UUID): Option[SourceRepository]

  def save(projectId: UUID, sourceRepository: SourceRepository): Unit

  def deleteByProjectId(projectId: UUID): Int
}
