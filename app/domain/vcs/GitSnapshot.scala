package net.mtgto.carpenter.domain.vcs

/**
 * Git revision and branch name
 * @param name
 * @param revision
 */
case class GitBranchSnapshot(
  name: String,
  revision: String
) extends Snapshot

/**
 * Git revision and tag name
 * @param name
 * @param revision
 */
case class GitTagSnapshot(
  name: String,
  revision: String
) extends Snapshot
