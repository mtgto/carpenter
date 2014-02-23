package net.mtgto.carpenter.infrastructure

import java.util.UUID
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import net.mtgto.carpenter.infrastructure.vcs.{SubversionPaths, SubversionPath}

class DatabaseSourceRepositoryDao extends SourceRepositoryDao {
  import play.api.Play.current

  private[this] val sourceRepositories = TableQuery[SourceRepositories]

  private[this] val subversionPaths = TableQuery[SubversionPaths]

//  protected[this] def convertRowToSourceRepository(row: Row): SourceRepository = {
//    row match {
//      // TODO use Enumeration for git/subversion
//      case Row("git", url: String) => GitSourceRepository(new URI(url))
//      case Row("subversion", url: String) => SubversionSourceRepository(new URI(url), Seq.empty)
//    }
//  }

//  protected[this] def convertSubversionPathsRowToPath(row: Row): SubversionPath = {
//    row match {
//      case Row(path: String, isDirectory: Boolean) => SubversionPath(path, isDirectory)
//    }
//  }

  override def findByProjectId(id: String): Option[(SourceRepository, Seq[SubversionPath])] = {
    DB.withTransaction { implicit session =>
      val query = for {
        sourceRepository <- sourceRepositories if sourceRepository.projectId === id
      } yield sourceRepository
      val subQuery = for {
        path <- subversionPaths if path.projectId === id
      } yield path
      query.firstOption.map(r => (r, subQuery.list))
    }
//    DB.withTransaction{ implicit c =>
//      val sourceRepository = SQL(
//        "SELECT `software`, `url` FROM `source_repositories` WHERE `project_id` = {projectId}")
//      .on('projectId -> id.toString)().map(convertRowToSourceRepository).headOption
//      sourceRepository match {
//        case Some(_: GitSourceRepository) => sourceRepository
//        case Some(sourceRepository: SubversionSourceRepository) =>
//          val paths = SQL(
//            """
//              |SELECT `path`, `directory` FROM `subversion_paths` WHERE `project_id` = {projectId};
//            """.stripMargin
//          ).on('projectId -> id.toString)().map(convertSubversionPathsRowToPath).toList
//          Some(SubversionSourceRepository(sourceRepository.uri, paths))
//        case _ => None
//      }
//    }
  }

  override def save(sourceRepository: SourceRepository, subversionPaths: Seq[SubversionPath]) {
    DB.withTransaction { implicit session: Session =>
      val rowCount = sourceRepositories.where(_.projectId === sourceRepository.projectId)
        .map(r => (r.software, r.url))
        .update((sourceRepository.software, sourceRepository.url))
      if (rowCount == 0) {
        sourceRepositories.insert(
          SourceRepository(sourceRepository.projectId, sourceRepository.software, sourceRepository.url))
      }
      TableQuery[SubversionPaths].filter(_.projectId === sourceRepository.projectId).delete
      TableQuery[SubversionPaths].insertAll(subversionPaths:_*)
      // TODO: SubversionPath
//      if (sourceRepository.software == "subversion") {
//        SubversionPaths.insert(SubversionPath(sourceRepository.projectId))
//      }
    }
//    DB.withTransaction{ implicit c =>
//      val rowCount =
//        SQL("UPDATE `source_repositories` SET `software` = {software}, `url` = {url} WHERE `project_id` = {projectId}")
//          .on('projectId -> projectId.toString, 'software -> sourceRepository.software, 'url -> sourceRepository.uri.toString)
//          .executeUpdate()
//      if (rowCount == 0)
//        SQL("INSERT INTO `source_repositories` (`project_id`, `software`, `url`) VALUES ({projectId},{software},{url})")
//          .on('projectId -> projectId.toString, 'software -> sourceRepository.software, 'url -> sourceRepository.uri.toString)
//          .executeInsert()
//      println("sourceRepository = "+ sourceRepository)
//      sourceRepository match {
//        case sourceRepository: SubversionSourceRepository => {
//          println("delete from")
//          SQL("DELETE FROM `subversion_paths` WHERE `project_id` = {projectId}"
//          ).on('projectId -> projectId.toString).executeUpdate()
//          val query = SQL("INSERT INTO `subversion_paths` (`project_id`, `path`, `directory`) VALUES ({projectId}, {path}, {isDirectory});")
//          println("query = " + query)
//          println("paths = " + sourceRepository.paths)
//          sourceRepository.paths.foreach { path =>
//            println("path = " + path)
//            //println("query = " + query.on('projectId -> projectId.toString, 'path -> "hogefuga", 'isDirectory -> 1).getFilledStatement(c, true))
//            //println("query = " + query.on('projectId -> projectId.toString, 'path -> path.path, 'isDirectory -> path.isDirectory).getFilledStatement(c, true))
//            query.on('projectId -> projectId.toString, 'path -> path.path, 'isDirectory -> path.isDirectory).executeInsert()
//          }
//        }
//      }
//    }
  }

  override def deleteByProjectId(projectId: String): Int = {
    DB.withTransaction { implicit session: Session =>
      //SubversionPaths.where(_.projectId === projectId).delete
      val query = for {
        sourceRepository <- sourceRepositories if sourceRepository.projectId === projectId
      } yield sourceRepository
      query.delete
    }
//    DB.withTransaction{ implicit c =>
//      SQL("DELETE FROM `subversion_paths WHERE `project_id` = {projectId}")
//        .on('projectId -> projectId.toString).executeUpdate()
//      SQL("DELETE FROM `source_repositories` WHERE `project_id` = {projectId}")
//        .on('projectId -> projectId.toString).executeUpdate()
//    }
  }
}
