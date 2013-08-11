package net.mtgto.carpenter.infrastructure

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

import java.util.UUID

class DatabaseUserDao extends UserDao {
  import play.api.Play.current

  override def findById(id: String): Option[(User, Authority)] = {
    DB.withSession { implicit session =>
      val query = for {
        user <- Users if user.id === id
        authority <- Authorities if authority.userId === id
      } yield (user, authority)
      query.firstOption
    }
  }

  override def findByNameAndPassword(name: String, password: String): Option[(User, Authority)] = {
    DB.withSession { implicit session =>
      val query = for {
        user <- Users if user.name === name && user.password === password
        authority <- Authorities if authority.userId === user.id
      } yield (user, authority)
      query.firstOption
    }
  }

  override def findAll: Seq[(User, Authority)] = {
    DB.withSession { implicit session =>
      val query = for {
        user <- Users
        authority <- Authorities if authority.userId === user.id
      } yield (user, authority)
      query.list
    }
  }

  override def save(id: String, name: String, password: String, authority: Authority): Unit = {
    DB.withTransaction { implicit session: Session =>
      val query = Query(Users).where(_.id === id)
    try {
      val rowCount = query.map(user => user.name ~ user.password).update((name, password))
      if (rowCount == 0) {
        Users.insert(User(id, name, password))
      }
    } catch {
      case e: Throwable => println(e.printStackTrace())
    }
      val subQuery = Authorities.where(_.userId === id)
      val subRowCount = subQuery.map(authority => authority.canLogin ~ authority.canCreateUser)
      .update((authority.canLogin, authority.canCreateUser))
      if (subRowCount == 0) {
        Authorities.insert(authority)
      }
    }
  }

  override def delete(id: String): Int = {
    DB.withTransaction { implicit session =>
      Authorities.where(_.userId === id).delete
      Users.where(_.id === id).delete
    }
  }
}
