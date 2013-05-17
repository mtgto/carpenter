package net.mtgto.carpenter.domain.vcs

import java.net.URI

case class SubversionSourceRepository(
  uri: URI,
  paths: Seq[SubversionPath]
) extends SourceRepository {
  override val sourceRepositoryType = SourceRepositoryType.Subversion

  def getPathsString: String = paths.map {path =>
    path.pathType match {
      case SubversionPathType.Parent => path.name + "/"
      case SubversionPathType.Child => path.name
    }
  }.mkString("\n")
}
