package net.mtgto.carpenter.domain.vcs

import java.net.URI
import net.mtgto.carpenter.domain.BranchType

case class SubversionSnapshot(
  name: String,
  branchType: BranchType.Value,
  uri: URI,
  revision: Long
) extends Snapshot
