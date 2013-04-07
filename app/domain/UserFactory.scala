package net.mtgto.domain

import java.util.UUID
import scalaz.Identity

trait UserFactory {
  def createUser(name: String, password: String, authority: Authority): User
}

object UserFactory extends UserFactory {
  override def createUser(name: String, password: String, authority: Authority): User = {
    User(Identity(UUID.randomUUID), name, Some(password), authority)
  }
}
