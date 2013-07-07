package net.mtgto.carpenter.domain

import net.mtgto.carpenter.infrastructure.{UserDao, DatabaseUserDao, Authority => InfraAuthority, User => InfraUser}
import org.sisioh.dddbase.core.lifecycle.{RepositoryWithEntity, EntityNotFoundException, Repository}
import scala.util.Try

trait UserRepository extends Repository[UserRepository, UserId, User] with BaseEntityReader[UserId, User] {
  def findByNameAndPassword(name: String, password: String): Option[User]
  def findAll: Seq[User]
}

object UserRepository {
  def apply(): UserRepository = new UserRepository {
    private val userDao: UserDao = new DatabaseUserDao

    override def findByNameAndPassword(name: String, password: String): Option[User] = {
      userDao.findByNameAndPassword(name, password).map(UserFactory.apply)
    }

    override def findAll: Seq[User] = {
      userDao.findAll.map(UserFactory.apply)
    }

    /**
     * 識別子に該当するエンティティを解決する。
     *
     * @param identity 識別子
     * @return Success:
     *         エンティティ
     *         Failure:
     *         EntityNotFoundExceptionは、エンティティが見つからなかった場合
     *         RepositoryExceptionは、リポジトリにアクセスできなかった場合。
     */
    override def resolve(identity: UserId): Try[User] = {
      Try(userDao.findById(identity.value.uuid).map(UserFactory.apply).getOrElse(throw new EntityNotFoundException))
    }

    /**
     * エンティティを保存する。
     *
     * @param entity 保存する対象のエンティティ
     * @return Success:
     *         リポジトリインスタンス
     *         Failure
     *         RepositoryExceptionは、リポジトリにアクセスできなかった場合。
     */
    def store(entity: User): Try[RepositoryWithEntity[UserRepository, User]] = {
      Try {
        assert(entity.password.isDefined)
        val infraAuthority = InfraAuthority(canLogin = entity.authority.canLogin, canCreateUser = entity.authority.canCreateUser)
        userDao.save(entity.identity.value.uuid, entity.name, entity.password.get, infraAuthority)
        RepositoryWithEntity(this, entity)
      }
    }

    /**
     * 指定した識別子のエンティティを削除する。
     *
     * @param identity 識別子
     * @return Success:
     *          リポジトリインスタンス
     *         Failure:
     *          RepositoryExceptionは、リポジトリにアクセスできなかった場合。
     */
    override def delete(identity: UserId): Try[UserRepository] = {
      Try {
        if (userDao.delete(identity.value.uuid) == 0) {
          throw new EntityNotFoundException
        }
        this
      }
    }

    override def contains(identity: UserId): Try[Boolean] = ???
  }
}
