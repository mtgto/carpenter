package net.mtgto.carpenter.domain

import java.util.UUID
import org.sisioh.dddbase.core.{Identity, Entity}

case class UserId(uuid: UUID) extends Identity[UserId] {
  override def value = this
}

/**
 * User
 *
 * @param identity identity
 * @param name name
 */
trait User extends Entity[UserId] {
  override val identity: Identity[UserId]
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
