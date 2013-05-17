package net.mtgto.carpenter.domain.vcs

import java.net.URI

trait SourceRepository {
  val sourceRepositoryType: SourceRepositoryType.Value
  val uri: URI
}
