package net.mtgto.carpenter.domain.vcs

import net.mtgto.carpenter.domain.BranchType

/**
 * Git revision and branch name
 * @param name
 * @param revision
 */
case class GitBranchSnapshot(
  name: String,
  revision: String
) extends Snapshot {
  override val branchType = BranchType.Branch
}

/**
 * Git revision and tag name
 * @param name
 * @param revision
 */
case class GitTagSnapshot(
  name: String,
  revision: String
) extends Snapshot {
  override val branchType = BranchType.Tag
}
