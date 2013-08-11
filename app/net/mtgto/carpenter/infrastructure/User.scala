package net.mtgto.carpenter.infrastructure

import java.util.UUID
import play.api.db.slick.Config.driver.simple._

case class User(id: String, name: String, password: String)

object Users extends Table[User]("users") {
  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name", O.NotNull)
  def password = column[String]("password", O.NotNull)
  def * = id ~ name ~ password <> (User.apply _, User.unapply _)
  def authority = Authorities.where(_.userId === id)
}
