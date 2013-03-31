package net.mtgto.domain

import java.util.UUID
import net.mtgto.infrastructure.{ProjectDao, DatabaseProjectDao}
import org.sisioh.dddbase.core.{EntityNotFoundException, Repository}
import scalaz.Identity

trait ProjectRepository extends Repository[Project, UUID] {
  def findAll: Seq[Project]
}

object ProjectRepository {
  def apply(): ProjectRepository = new ProjectRepository {
    private val projectDao: ProjectDao = new DatabaseProjectDao

    override def findAll: Seq[Project] = {
      projectDao.findAll.map {
        infraProject => Project(Identity(infraProject.id), infraProject.name, infraProject.hostname, infraProject.recipe)
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
    override def resolve(identifier: Identity[UUID]): Project = {
      resolveOption(identifier).getOrElse(throw new EntityNotFoundException)
    }

    override def resolveOption(identifier: Identity[UUID]): Option[Project] = {
      projectDao.findById(identifier.value).map {
        infraProject => Project(Identity(infraProject.id), infraProject.name, infraProject.hostname, infraProject.recipe)
      }
    }

    override def contains(identifier: Identity[UUID]): Boolean = {
      resolveOption(identifier).isDefined
    }

    override def contains(entity: Project): Boolean = {
      resolveOption(entity.identity).isDefined
    }

    /**
     * エンティティを保存する。
     *
     * @param entity 保存する対象のエンティティ
     * @throws RepositoryException リポジトリにアクセスできない場合
     */
    override def store(entity: Project): Unit = {
      projectDao.save(entity.identity.value, entity.name, entity.hostname, entity.recipe)
    }

    /**
     * 指定した識別子のエンティティを削除する。
     *
     * @param identity 識別子
     * @throws EntityNotFoundException 指定された識別子を持つエンティティが見つからなかった場合
     * @throws RepositoryException リポジトリにアクセスできない場合
     */
    override def delete(identity: Identity[UUID]): Unit = {
      if (projectDao.delete(identity.value) == 0) {
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
    override def delete(entity: Project): Unit = {
      delete(entity.identity)
    }
  }
}
