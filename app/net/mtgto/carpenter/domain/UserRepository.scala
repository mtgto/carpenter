package net.mtgto.carpenter.domain

import net.mtgto.carpenter.infrastructure.{UserDao, DatabaseUserDao, Authority => InfraAuthority, User => InfraUser}
import org.sisioh.dddbase.core.lifecycle.{ResultWithEntity, EntityNotFoundException}
import org.sisioh.dddbase.core.lifecycle.sync.{SyncResultWithEntity, SyncRepository}
import scala.util.Try

trait UserRepository extends SyncRepository[UserId, User] with BaseEntityReader[UserId, User] {
  def findByNameAndPassword(name: String, password: String): Option[User]
  def findAll: Seq[User]
}

object UserRepository {
  def apply(): UserRepository = new UserRepository {
    type This = UserRepository

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
      Try(userDao.findById(identity.uuid.toString).map(UserFactory.apply).getOrElse(throw new EntityNotFoundException))
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
    def store(entity: User): Try[ResultWithEntity[This, UserId, User, Try]] = {
      Try {
        assert(entity.password.isDefined)
        val infraAuthority = InfraAuthority(entity.identity.uuid.toString, canLogin = entity.authority.canLogin,
          canCreateUser = entity.authority.canCreateUser)
        userDao.save(entity.identity.uuid.toString, entity.name, entity.password.get, infraAuthority)
        SyncResultWithEntity(this, entity)
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
    override def delete(identity: UserId): Try[This] = {
      Try {
        if (userDao.delete(identity.uuid.toString) == 0) {
          throw new EntityNotFoundException
        }
        this
      }
    }

    override def contains(identity: UserId): Try[Boolean] = ???
  }
}
