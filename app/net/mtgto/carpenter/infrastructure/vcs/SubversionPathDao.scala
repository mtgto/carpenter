package net.mtgto.carpenter.infrastructure.vcs

import java.util.UUID

trait SubversionPathDao {
  def findAllByProjectId(projectId: String): Seq[SubversionPath]
}
