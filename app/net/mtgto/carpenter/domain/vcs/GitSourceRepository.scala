package net.mtgto.carpenter.domain.vcs

import java.net.URI

case class GitSourceRepository(
  uri: URI
) extends SourceRepository {
  override val sourceRepositoryType = SourceRepositoryType.Git
}
