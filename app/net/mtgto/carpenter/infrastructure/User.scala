package net.mtgto.carpenter.infrastructure

import java.util.UUID
import play.api.db.slick.Config.driver.simple._

case class User(id: String, name: String, password: String)

class Users(tag: Tag) extends Table[User](tag, "users") {
  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name", O.NotNull)
  def password = column[String]("password", O.NotNull)
  def * = (id, name, password) <> (User.tupled, User.unapply)
  def authority = TableQuery[Authorities].filter(_.userId === id)
}
