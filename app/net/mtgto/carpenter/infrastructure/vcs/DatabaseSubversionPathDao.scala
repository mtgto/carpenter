package net.mtgto.carpenter.infrastructure.vcs

import java.util.UUID
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

class DatabaseSubversionPathDao extends SubversionPathDao {
  import play.api.Play.current

  private[this] val subversionPaths = TableQuery[SubversionPaths]
//  protected[this] def convertRowToSubversionPath(row: Row): SubversionPath = {
//    row match {
//      case Row(path: String, isDirectory: Boolean) =>
//        SubversionPath(path, isDirectory)
//    }
//  }

  override def findAllByProjectId(projectId: String): Seq[SubversionPath] = {
    DB.withSession { implicit session =>
      val query = for {
        path <- subversionPaths if path.projectId === projectId
      } yield path
      query.list
    }
//    DB.withConnection{ implicit c =>
//      SQL("SELECT `path`, `is_directory` FROM `subversion_paths` WHERE `project_id` = {id}")
//        .on('id -> projectId.toString)()
//        .map(convertRowToSubversionPath)
//        .toList
//    }
  }
}
