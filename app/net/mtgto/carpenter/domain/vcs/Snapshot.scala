package net.mtgto.carpenter.domain.vcs

trait Snapshot {
  val branchType: BranchType.Value
  val name: String
  val revision: Revision
}
