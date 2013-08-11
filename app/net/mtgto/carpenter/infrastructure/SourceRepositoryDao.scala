package net.mtgto.carpenter.infrastructure

import java.util.UUID
import net.mtgto.carpenter.infrastructure.vcs.SubversionPath

trait SourceRepositoryDao {
  def findByProjectId(id: String): Option[(SourceRepository, Seq[SubversionPath])]

  def save(sourceRepository: SourceRepository, subversionPath: Seq[SubversionPath]): Unit

  def deleteByProjectId(projectId: String): Int
}
