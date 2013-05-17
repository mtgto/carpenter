package net.mtgto.carpenter.infrastructure.vcs

import anorm._
import java.util.UUID
import play.api.db.DB

class DatabaseSubversionPathDao extends SubversionPathDao {
  import play.api.Play.current
  protected[this] def convertRowToSubversionPath(row: Row): SubversionPath = {
    row match {
      case Row(path: String, isDirectory: Boolean) =>
        SubversionPath(path, isDirectory)
    }
  }

  override def findAllByProjectId(projectId: UUID): Seq[SubversionPath] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `path`, `is_directory` FROM `subversion_paths` WHERE `project_id` = {id}")
        .on('id -> projectId.toString)()
        .map(convertRowToSubversionPath)
        .toList
    }
  }
}
