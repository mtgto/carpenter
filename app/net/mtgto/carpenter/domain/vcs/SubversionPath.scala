package net.mtgto.carpenter.domain.vcs

case class SubversionPath(
  pathType: SubversionPathType.Value,
  name: String
)
