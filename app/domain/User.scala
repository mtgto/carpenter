package net.mtgto.domain

import java.util.UUID
import org.sisioh.dddbase.core.Entity
import scalaz.Identity

/**
 * User
 *
 * @param identity identity
 * @param name name
 */
trait User extends Entity[UUID] {
  override val identity: Identity[UUID]
  val name: String
  val password: Option[String]

  override def toString: String = Seq(identity, name).mkString("User(", ", ",")")
}

object User {
  private case class DefaultUser(identity: Identity[UUID], name: String, password: Option[String]) extends User

  def apply(identity: Identity[UUID], name: String, password: Option[String]): User = {
    DefaultUser(identity, name, password)
  }
}
