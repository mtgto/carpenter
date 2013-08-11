package net.mtgto.carpenter.domain

import java.util.UUID
import net.mtgto.carpenter.infrastructure.{User => InfraUser, Authority => InfraAuthority}

trait UserFactory {
  def createUser(name: String, password: String, authority: Authority): User
  def apply(infraUserWithAuthority: (InfraUser, InfraAuthority)): User
}

object UserFactory extends UserFactory {
  override def createUser(name: String, password: String, authority: Authority): User = {
    User(UserId(UUID.randomUUID), name, Some(password), authority)
  }

  override def apply(infraUserWithAuthority: (InfraUser, InfraAuthority)): User = {
    val (infraUser, infraAuthority) = infraUserWithAuthority
    User(UserId(UUID.fromString(infraUser.id)), infraUser.name, None, convertInfraAuthorityToDomain(infraAuthority))
  }

  protected[this] def convertInfraAuthorityToDomain(infraAuthority: InfraAuthority): Authority = {
    Authority(canLogin = infraAuthority.canLogin, canCreateUser = infraAuthority.canCreateUser)
  }
}
