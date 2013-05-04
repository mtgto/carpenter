package net.mtgto.carpenter.domain

import java.util.UUID

trait UserFactory {
  def createUser(name: String, password: String, authority: Authority): User
}

object UserFactory extends UserFactory {
  override def createUser(name: String, password: String, authority: Authority): User = {
    User(UserId(UUID.randomUUID), name, Some(password), authority)
  }
}
