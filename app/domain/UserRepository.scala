package net.mtgto.domain

import java.util.UUID
import net.mtgto.infrastructure.{UserDao, DatabaseUserDao}
import org.sisioh.dddbase.core.{EntityNotFoundException, Repository}
import scalaz.Identity

trait UserRepository extends Repository[User, UUID] {
  def findByNameAndPassword(name: String, password: String): Option[User]
  def findAll: Seq[User]
}

object UserRepository {
  def apply(): UserRepository = new UserRepository {
    private val userDao: UserDao = new DatabaseUserDao

    override def findByNameAndPassword(name: String, password: String): Option[User] = {
      userDao.findByNameAndPassword(name, password).map {
        infraUser => User(Identity(infraUser.id), infraUser.name, None)
      }
    }

    override def findAll: Seq[User] = {
      userDao.findAll.map {
        infraUser => User(Identity(infraUser.id), infraUser.name, None)
      }
    }

    /**
     * 識別子に該当するエンティティを取得する。
     *
     *  @param identifier 識別子
     *  @return エンティティ
     *
     *  @throws IllegalArgumentException
     *  @throws EntityNotFoundException エンティティが見つからなかった場合
     *  @throws RepositoryException リポジトリにアクセスできない場合
     */
    override def resolve(identifier: Identity[UUID]): User = {
      resolveOption(identifier).getOrElse(throw new EntityNotFoundException)
    }

    override def resolveOption(identifier: Identity[UUID]): Option[User] = {
      userDao.findById(identifier.value).map {
        infraUser => User(Identity(infraUser.id), infraUser.name, None)
      }
    }

    override def contains(identifier: Identity[UUID]): Boolean = {
      resolveOption(identifier).isDefined
    }

    override def contains(entity: User): Boolean = {
      resolveOption(entity.identity).isDefined
    }

    /**
     * エンティティを保存する。
     *
     * @param entity 保存する対象のエンティティ
     * @throws RepositoryException リポジトリにアクセスできない場合
     */
    override def store(entity: User): Unit = {
      assert(entity.password.isDefined)
      userDao.save(entity.identity.value, entity.name, entity.password.get)
    }

    /**
     * 指定した識別子のエンティティを削除する。
     *
     * @param identity 識別子
     * @throws EntityNotFoundException 指定された識別子を持つエンティティが見つからなかった場合
     * @throws RepositoryException リポジトリにアクセスできない場合
     */
    override def delete(identity: Identity[UUID]): Unit = {
      if (userDao.delete(identity.value) == 0) {
        throw new EntityNotFoundException
      }
    }

    /**
     * 指定したエンティティを削除する。
     *
     * @param entity エンティティ
     * @throws EntityNotFoundException 指定された識別子を持つエンティティが見つからなかった場合
     * @throws RepositoryException リポジトリにアクセスできない場合
     */
    override def delete(entity: User): Unit = {
      delete(entity.identity)
    }
  }
}
