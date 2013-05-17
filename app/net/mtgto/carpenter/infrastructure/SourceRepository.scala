package net.mtgto.carpenter.infrastructure

import java.net.URI
import net.mtgto.carpenter.infrastructure.vcs.SubversionPath

trait SourceRepository {
  val software: String
  val uri: URI
}

case class GitSourceRepository(uri: URI) extends SourceRepository {
  override val software: String = "git"
}

case class SubversionSourceRepository(uri: URI, paths: Seq[SubversionPath]) extends SourceRepository {
  override val software: String = "subversion"
}
