package net.mtgto.carpenter.domain

import java.util.UUID
import net.mtgto.carpenter.infrastructure.{UserDao, DatabaseUserDao, Authority => InfraAuthority, User => InfraUser}
import org.sisioh.dddbase.core.{Identity, EntityNotFoundException, Repository}
import scala.util.Try

trait UserRepository extends Repository[UserId, User] {
  def findByNameAndPassword(name: String, password: String): Option[User]
  def findAll: Seq[User]
}

object UserRepository {
  def apply(): UserRepository = new UserRepository {
    private val userDao: UserDao = new DatabaseUserDao

    protected[this] def convertInfraAuthorityToDomain(infraAuthority: InfraAuthority): Authority = {
      Authority(canLogin = infraAuthority.canLogin, canCreateUser = infraAuthority.canCreateUser)
    }

    protected[this] def convertInfraToDomain(infraUser: InfraUser): User = {
      User(UserId(infraUser.id), infraUser.name, None, convertInfraAuthorityToDomain(infraUser.authority))
    }

    override def findByNameAndPassword(name: String, password: String): Option[User] = {
      userDao.findByNameAndPassword(name, password).map(convertInfraToDomain)
    }

    override def findAll: Seq[User] = {
      userDao.findAll.map(convertInfraToDomain)
    }

    /**
     * 識別子に該当するエンティティを取得する。
     *
     * @param identity 識別子
     * @return Success:
     *          エンティティ
     *         Failure:
     *          EntityNotFoundExceptionは、エンティティが見つからなかった場合
     *          RepositoryExceptionは、リポジトリにアクセスできなかった場合。
     */
    override def resolve(identity: Identity[UserId]): Try[User] = {
      Try(resolveOption(identity).getOrElse(throw new EntityNotFoundException))
    }

    /**
     * 識別子に該当するエンティティを取得する。
     *
     * @param identity 識別子
     * @return Option[T]
     */
    override def resolveOption(identity: Identity[UserId]): Option[User] = {
      userDao.findById(identity.value.uuid).map(convertInfraToDomain)
    }

    /**
     * 指定した識別子のエンティティが存在するかを返す。
     *
     * @param identifier 識別子
     * @return Success:
     *          存在する場合はtrue
     *         Failure:
     *          RepositoryExceptionは、リポジトリにアクセスできなかった場合。
     */
    override def contains(identifier: Identity[UserId]): Try[Boolean] = {
      Try(resolveOption(identifier).isDefined)
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
    override def delete(identity: Identity[UserId]): Try[UserRepository] = {
      Try {
        if (userDao.delete(identity.value.uuid) == 0) {
          throw new EntityNotFoundException
        }
        this
      }
    }
  }
}
