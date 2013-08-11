package net.mtgto.carpenter.infrastructure.vcs

import java.util.UUID
import play.api.db.slick.Config.driver.simple._
import net.mtgto.carpenter.infrastructure.Projects

/**
 * Path for subversion (ex. "branches/feature1", "trunk")
 * @param projectId
 * @param path path to the directory
 * @param isDirectory true if this path is contains subdirectories
 */
case class SubversionPath(
  projectId: String,
  path: String,
  isDirectory: Boolean
)

object SubversionPaths extends Table[SubversionPath]("subversion_paths") {
  def projectId = column[String]("project_id", O.DBType("CHAR(36)"), O.NotNull)
  def path = column[String]("path", O.DBType("VARCHAR(255)"), O.NotNull)
  def isDirectory = column[Boolean]("directory", O.DBType("BOOLEAN"), O.NotNull)
  def * = projectId ~ path ~ isDirectory <> (SubversionPath.apply _, SubversionPath.unapply _)
  def idx = index("project_id_and_path", (projectId, path), unique = true)
  def project = foreignKey("project_id", projectId, Projects)(_.id)
}
