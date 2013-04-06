package net.mtgto.domain

import java.util.UUID
import net.mtgto.infrastructure.{JobDao, DatabaseJobDao}
import org.sisioh.dddbase.core.{EntityNotFoundException, Repository}
import scalaz.Identity

trait JobRepository extends Repository[Job, UUID] {
  def findAll: Seq[Job]
}

object JobRepository {
  def apply(): JobRepository = new JobRepository {
    private val jobDao: JobDao = new DatabaseJobDao

    protected[this] val projectRepository: ProjectRepository = ProjectRepository()

    override def findAll: Seq[Job] = {
      jobDao.findAll.map {
        infraJob => Job(Identity(infraJob.id), projectRepository.resolve(Identity(infraJob.projectId)), infraJob.exitCode, infraJob.log)
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
    override def resolve(identifier: Identity[UUID]): Job = {
      resolveOption(identifier).getOrElse(throw new EntityNotFoundException)
    }

    override def resolveOption(identifier: Identity[UUID]): Option[Job] = {
      jobDao.findById(identifier.value).map {
        infraJob => Job(Identity(infraJob.id), projectRepository.resolve(Identity(infraJob.projectId)), infraJob.exitCode, infraJob.log)
      }
    }

    override def contains(identifier: Identity[UUID]): Boolean = {
      resolveOption(identifier).isDefined
    }

    override def contains(entity: Job): Boolean = {
      resolveOption(entity.identity).isDefined
    }

    /**
     * エンティティを保存する。
     *
     * @param entity 保存する対象のエンティティ
     * @throws RepositoryException リポジトリにアクセスできない場合
     */
    override def store(entity: Job): Unit = {
      jobDao.save(entity.identity.value, entity.project.identity.value, entity.exitCode, entity.log)
    }

    /**
     * 指定した識別子のエンティティを削除する。
     *
     * @param identity 識別子
     * @throws EntityNotFoundException 指定された識別子を持つエンティティが見つからなかった場合
     * @throws RepositoryException リポジトリにアクセスできない場合
     */
    override def delete(identity: Identity[UUID]): Unit = {
      if (jobDao.delete(identity.value) == 0) {
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
    override def delete(entity: Job): Unit = {
      delete(entity.identity)
    }
  }
}