package net.mtgto.carpenter.domain

import net.mtgto.carpenter.infrastructure.{Project => InfraProject, ProjectDao, DatabaseProjectDao}
import org.sisioh.dddbase.core.lifecycle.{ResultWithEntity, EntityNotFoundException}
import org.sisioh.dddbase.core.lifecycle.sync.{SyncResultWithEntity, SyncRepository}
import scala.util.Try
import net.mtgto.carpenter.domain.vcs.SourceRepositoryService

trait ProjectRepository extends SyncRepository[ProjectId, Project] with BaseEntityReader[ProjectId, Project] {
  def findAll: Seq[Project]
}

object ProjectRepository {
  def apply(): ProjectRepository = new ProjectRepository {
    override type This = ProjectRepository

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
     *         エンティティ
     *         Failure:
     *         EntityNotFoundExceptionは、エンティティが見つからなかった場合
     *         RepositoryExceptionは、リポジトリにアクセスできなかった場合。
     */
    override def resolve(identity: ProjectId): Try[Project] = {
      Try(projectDao.findById(identity.uuid.toString).map(ProjectFactory.apply).getOrElse(throw new EntityNotFoundException))
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
    def store(entity: Project): Try[ResultWithEntity[This, ProjectId, Project, Try]] = {
      Try {
        projectDao.save(entity.identity.uuid.toString, entity.name, entity.hostname, entity.recipe)
        sourceRepositoryService.save(entity.identity, entity.sourceRepository)
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
    override def delete(identity: ProjectId): Try[This] = {
      Try {
        if (projectDao.delete(identity.uuid.toString) == 0) {
          throw new EntityNotFoundException
        }
        this
      }
    }
  }
}
