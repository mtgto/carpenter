package net.mtgto.carpenter.infrastructure

import play.api.db.slick.Config.driver.simple._
import java.util.UUID

case class Authority(userId: String, canLogin: Boolean, canCreateUser: Boolean)

class Authorities(tag: Tag) extends Table[Authority](tag, "authorities") {
  def userId = column[String]("user_id", O.NotNull)
  def canLogin = column[Boolean]("can_login", O.NotNull, O.Default(true))
  def canCreateUser = column[Boolean]("can_create_user", O.NotNull, O.Default(false))
  def * = (userId, canLogin, canCreateUser) <> (Authority.tupled, Authority.unapply)
  def user = foreignKey("user_id", userId, TableQuery[Users])(_.id)
}
