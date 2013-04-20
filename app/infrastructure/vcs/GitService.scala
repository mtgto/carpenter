package net.mtgto.carpenter.infrastructure.vcs

import java.net.URI
import scala.sys.process.Process
import scala.util.Try

class GitService extends VCSService {
  private val command = "git"

  def getBranchRevision(uri: URI, branchName: String): Try[GitRevision] = {
    Try {
      // ProcessBuilder#!! throws an Exception when exit code != 0
      Process(Seq(command, "ls-remote", "--heads", uri.toString)).lines.map {
        line =>
          val Array(revision, name) = line.split("""\s+""")
          GitRevision(name = name, revision = revision)
      }.find(_.name == branchName).get
    }
  }

  def getTagRevision(uri: URI, tagName: String): Try[GitRevision] = {
    Try {
      // ProcessBuilder#!! throws an Exception when exit code != 0
      Process(Seq(command, "ls-remote", "--tags", uri.toString)).lines.map {
        line =>
          val Array(revision, name) = line.split("""\s+""")
          GitRevision(name = name, revision = revision)
      }.find(_.name == tagName).get
    }
  }
}
