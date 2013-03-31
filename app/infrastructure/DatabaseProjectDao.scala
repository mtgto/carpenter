package net.mtgto.infrastructure

import anorm._
import anorm.SqlParser._
import play.api.db.DB

import java.util.UUID

class DatabaseProjectDao extends ProjectDao {
  import play.api.Play.current

  protected[this] def convertRowToProject(row: Row): Project = {
    row match {
      case Row(id: String, name: String, hostname: String, recipe: java.sql.Clob) =>
        Project(UUID.fromString(id), name, hostname, recipe.getSubString(1, recipe.length.toInt))
    }
  }

  override def findById(id: UUID): Option[Project] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `name`, `hostname`, `recipe` FROM `projects` WHERE `id` = {id}").on("id" -> id)()
      .headOption.map(convertRowToProject)
    }
  }

  override def findAll: Seq[Project] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `id`, `name`, `hostname`, `recipe` FROM `projects`")().map(convertRowToProject).toList
    }
  }

  override def save(id: UUID, name: String, hostname: String, recipe: String): Unit = {
    DB.withConnection{ implicit c =>
      val rowCount =
        SQL("UPDATE `projects` SET `name` = {name}, `hostname` = {hostname}, `recipe` = {recipe} WHERE `id` = {id}")
          .on('id -> id, 'name -> name, 'hostname -> hostname, 'recipe -> recipe).executeUpdate()
      if (rowCount == 0)
        SQL("INSERT INTO `projects` (`id`, `name`, `hostname`, `recipe`) VALUES ({id},{name},{hostname},{recipe})")
          .on('id -> id, 'name -> name, 'hostname -> hostname, 'recipe -> recipe).executeInsert()
    }
  }

  override def delete(id: UUID): Unit = {
    DB.withConnection{ implicit c =>
      SQL("DELETE `projects` WHERE `id` = {id}")
        .on('id -> id).executeUpdate()
    }
  }
}
