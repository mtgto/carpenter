package net.mtgto.carpenter.domain

import java.util.UUID
import org.sisioh.dddbase.core.model.{Identity, Entity}

case class UserId(uuid: UUID) extends Identity[UUID] {
  override def value = uuid
}

/**
 * User
 *
 * @param identity identity
 * @param name name
 * @param password password. Non-empty when user is logging in. Empty after user is logged in.
 * @param authority user's authority
 */
trait User extends Entity[UserId] {
  override val identity: UserId
  val name: String
  val password: Option[String]
  val authority: Authority

  override def toString: String = Seq(identity, name, authority).mkString("User(", ", ",")")
}

object User {
  private case class DefaultUser(identity: UserId, name: String, password: Option[String], authority: Authority) extends User

  def apply(identity: UserId, name: String, password: Option[String], authority: Authority): User = {
    DefaultUser(identity, name, password, authority)
  }
}
