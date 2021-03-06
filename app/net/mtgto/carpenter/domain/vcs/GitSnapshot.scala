package net.mtgto.carpenter.domain.vcs

/**
 * Git revision and branch name
 * @param name
 * @param revision
 */
case class GitBranchSnapshot(
  name: String,
  revision: GitRevision
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
  revision: GitRevision
) extends Snapshot {
  override val branchType = BranchType.Tag
}
