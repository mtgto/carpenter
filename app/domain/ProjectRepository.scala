package net.mtgto.carpenter.domain

import net.mtgto.carpenter.infrastructure.{Project => InfraProject, ProjectDao, DatabaseProjectDao}
import org.sisioh.dddbase.core.{Identity, EntityNotFoundException, Repository}
import scala.util.Try

trait ProjectRepository extends Repository[ProjectId, Project] {
  def findAll: Seq[Project]
}

object ProjectRepository {
  def apply(): ProjectRepository = new ProjectRepository {
    private val projectDao: ProjectDao = new DatabaseProjectDao

    private val sourceRepositoryService: SourceRepositoryService = SourceRepositoryService

    private def convertInfraProjectToDomain(infraProject: InfraProject): Project = {
      Project(
        ProjectId(infraProject.id), infraProject.name, infraProject.hostname,
        sourceRepositoryService.get(ProjectId(infraProject.id)), infraProject.recipe)
    }

    override def findAll: Seq[Project] = {
      projectDao.findAll.map(convertInfraProjectToDomain)
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
    override def resolve(identity: Identity[ProjectId]): Try[Project] = {
      Try(resolveOption(identity).getOrElse(throw new EntityNotFoundException))
    }

    /**
     * 識別子に該当するエンティティを取得する。
     *
     * @param identity 識別子
     * @return Option[T]
     */
    override def resolveOption(identity: Identity[ProjectId]): Option[Project] = {
      projectDao.findById(identity.value.uuid).map(convertInfraProjectToDomain)
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
    override def contains(identifier: Identity[ProjectId]): Try[Boolean] = {
      Try(resolveOption(identifier).isDefined)
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
    override def delete(identity: Identity[ProjectId]): Try[ProjectRepository] = {
      Try {
        if (projectDao.delete(identity.value.uuid) == 0) {
          throw new EntityNotFoundException
        }
        this
      }
    }
  }
}
