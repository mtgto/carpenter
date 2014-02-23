package net.mtgto.carpenter.infrastructure

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

import java.util.UUID

class DatabaseProjectDao extends ProjectDao {
  import play.api.Play.current

  private[this] val projects = TableQuery[Projects]

//  protected[this] def convertRowToProject(row: Row): Project = {
//    row match {
//      case Row(id: String, name: String, hostname: String, recipe: java.sql.Clob) =>
//        Project(UUID.fromString(id), name, hostname, recipe.getSubString(1, recipe.length.toInt))
//    }
//  }

  override def findById(id: String): Option[Project] = {
    DB.withSession { implicit session =>
      val query = for {
        project <- projects if project.id === id
      } yield project
      query.firstOption
    }
  }

  override def findAll: Seq[Project] = {
    DB.withSession { implicit session =>
      val query = for {
        project <- projects
      } yield project
      query.list
    }
  }

  override def save(id: String, name: String, hostname: String, recipe: String) {
    DB.withSession { implicit session: Session =>
      val rowCount = projects.where(_.id === id)
        .map(p => (p.name, p.hostname, p.recipe))
        .update((name, hostname, recipe))
      if (rowCount == 0) {
        projects += Project(id, name, hostname, recipe)
      }
    }
  }

  override def delete(id: String): Int = {
    DB.withSession { implicit session =>
      val query = for {
        project <- projects if project.id === id
      } yield project
      query.delete
    }
  }
}
