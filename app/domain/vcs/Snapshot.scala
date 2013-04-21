package net.mtgto.carpenter.domain.vcs

import net.mtgto.carpenter.domain.BranchType

trait Snapshot {
  val branchType: BranchType.Value
  val name: String
}
