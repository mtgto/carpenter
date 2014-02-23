package net.mtgto.carpenter.infrastructure

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

import java.util.UUID

class DatabaseUserDao extends UserDao {
  import play.api.Play.current

  private[this] val users = TableQuery[Users]

  private[this] val authorities = TableQuery[Authorities]

  override def findById(id: String): Option[(User, Authority)] = {
    DB.withSession { implicit session =>
      val query = for {
        user <- users if user.id === id
        authority <- authorities if authority.userId === user.id
      } yield (user, authority)
      query.firstOption
    }
  }

  override def findByNameAndPassword(name: String, password: String): Option[(User, Authority)] = {
    DB.withSession { implicit session =>
      val query = for {
        user <- users if user.name === name && user.password === password
        authority <- authorities if authority.userId === user.id
      } yield (user, authority)
      query.firstOption
    }
  }

  override def findAll: Seq[(User, Authority)] = {
    DB.withSession { implicit session =>
      val query = for {
        user <- users
        authority <- authorities if authority.userId === user.id
      } yield (user, authority)
      query.list
    }
  }

  override def save(id: String, name: String, password: String, authority: Authority): Unit = {
    DB.withTransaction { implicit session: Session =>
      val query = users.where(_.id === id)
      val rowCount = query.map(user => (user.name, user.password)).update((name, password))
      if (rowCount == 0) {
        users += User(id, name, password)
      }
      val subQuery = authorities.where(_.userId === id)
      val subRowCount = subQuery.map(authority => (authority.canLogin, authority.canCreateUser))
      .update((authority.canLogin, authority.canCreateUser))
      if (subRowCount == 0) {
        authorities += authority
      }
    }
  }

  override def delete(id: String): Int = {
    DB.withTransaction { implicit session =>
      authorities.where(_.userId === id).delete
      users.where(_.id === id).delete
    }
  }
}
