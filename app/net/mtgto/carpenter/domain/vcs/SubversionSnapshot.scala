package net.mtgto.carpenter.domain.vcs

import java.net.URI

case class SubversionSnapshot(
  name: String,
  branchType: BranchType.Value,
  uri: URI,
  revision: SubversionRevision
) extends Snapshot
