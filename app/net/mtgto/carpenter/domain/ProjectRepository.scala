package net.mtgto.carpenter.domain

import net.mtgto.carpenter.infrastructure.{Project => InfraProject, ProjectDao, DatabaseProjectDao}
import org.sisioh.dddbase.core.{EntityNotFoundException, Repository}
import scala.util.Try
import net.mtgto.carpenter.domain.vcs.SourceRepositoryService

trait ProjectRepository extends Repository[ProjectId, Project] with BaseEntityResolver[ProjectId, Project] {
  def findAll: Seq[Project]
}

object ProjectRepository {
  def apply(): ProjectRepository = new ProjectRepository {
    private val projectDao: ProjectDao = new DatabaseProjectDao

    private val sourceRepositoryService: SourceRepositoryService = SourceRepositoryService

    override def findAll: Seq[Project] = {
      projectDao.findAll.map(ProjectFactory.apply)
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
    override def resolveOption(identity: ProjectId): Try[Option[Project]] = {
      Try(projectDao.findById(identity.value.uuid).map(ProjectFactory.apply))
    }

    /**
     * エンティティを保存する。
     *
     * @param entity 保存する対象のエンティティ
     * @return Success:
     *          リポジトリインスタンス
     *         Failure:
     *          RepositoryExceptionは、リポジトリにアクセスできなかった場合。
     */
    override def store(entity: Project): Try[ProjectRepository] = {
      Try {
        projectDao.save(entity.identity.uuid, entity.name, entity.hostname, entity.recipe)
        sourceRepositoryService.save(entity.identity, entity.sourceRepository)
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
    override def delete(identity: ProjectId): Try[ProjectRepository] = {
      Try {
        if (projectDao.delete(identity.value.uuid) == 0) {
          throw new EntityNotFoundException
        }
        this
      }
    }
  }
}
