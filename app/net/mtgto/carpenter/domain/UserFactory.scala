package net.mtgto.carpenter.domain

import java.util.UUID
import net.mtgto.carpenter.infrastructure.{User => InfraUser, Authority => InfraAuthority}

trait UserFactory {
  def createUser(name: String, password: String, authority: Authority): User
  def apply(infraProject: InfraUser): User
}

object UserFactory extends UserFactory {
  override def createUser(name: String, password: String, authority: Authority): User = {
    User(UserId(UUID.randomUUID), name, Some(password), authority)
  }

  override def apply(infraUser: InfraUser): User = {
    User(UserId(infraUser.id), infraUser.name, None, convertInfraAuthorityToDomain(infraUser.authority))
  }

  protected[this] def convertInfraAuthorityToDomain(infraAuthority: InfraAuthority): Authority = {
    Authority(canLogin = infraAuthority.canLogin, canCreateUser = infraAuthority.canCreateUser)
  }
}
