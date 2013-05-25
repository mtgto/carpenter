package net.mtgto.carpenter.domain

import net.mtgto.carpenter.infrastructure.{UserDao, DatabaseUserDao, Authority => InfraAuthority, User => InfraUser}
import org.sisioh.dddbase.core.{EntityNotFoundException, Repository}
import scala.util.Try

trait UserRepository extends Repository[UserId, User] with BaseEntityResolver[UserId, User] {
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
     *          Some: エンティティが存在する場合
     *          None: エンティティが存在しない場合
     *         Failure:
     *          RepositoryExceptionは、リポジトリにアクセスできなかった場合。
     */
    override def resolveOption(identity: UserId): Try[Option[User]] = {
      Try(userDao.findById(identity.value.uuid).map(UserFactory.apply))
    }

    /**
     * エンティティを保存する。
     *
     * @param entity 保存する対象のエンティティ
     * @throws RepositoryException リポジトリにアクセスできない場合
     */
    override def store(entity: User): Try[UserRepository] = {
      Try {
        assert(entity.password.isDefined)
        val infraAuthority = InfraAuthority(canLogin = entity.authority.canLogin, canCreateUser = entity.authority.canCreateUser)
        userDao.save(entity.identity.value.uuid, entity.name, entity.password.get, infraAuthority)
        this
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
  }
}
