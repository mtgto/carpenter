package net.mtgto.carpenter.domain

import net.mtgto.carpenter.infrastructure.{Job => InfraJob, JobDao, DatabaseJobDao}
import org.sisioh.baseunits.scala.time.{Duration, TimePoint}
import org.sisioh.dddbase.core.{Identity, EntityNotFoundException, Repository}
import scala.util.Try

trait JobRepository extends Repository[JobId, Job] {
  def findAll: Try[Seq[Job]]

  def findAllByProject(project: Project): Try[Seq[Job]]

  def findAllByProjectOrderByTimePointDesc(project: Project): Try[Seq[Job]]
}

object JobRepository {
  def apply(): JobRepository = new JobRepository {
    private val jobDao: JobDao = new DatabaseJobDao

    protected[this] val projectRepository: ProjectRepository = ProjectRepository()

    protected[this] val userRepository: UserRepository = UserRepository()

    override def findAll: Try[Seq[Job]] = {
      Try {
        jobDao.findAll.map {
          convertInfraToDomain(_).get
        }
      }
    }

    override def findAllByProject(project: Project): Try[Seq[Job]] = {
      Try {
        jobDao.findAllByProject(project.identity.uuid).map {
          convertInfraToDomain(_).get
        }
      }
    }

    override def findAllByProjectOrderByTimePointDesc(project: Project): Try[Seq[Job]] = {
      Try {
        jobDao.findAllByProjectOrderByDateDesc(project.identity.uuid).map {
          convertInfraToDomain(_).get
        }
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
    override def resolve(identifier: Identity[JobId]): Try[Job] = {
      Try(resolveOption(identifier).getOrElse(throw new EntityNotFoundException))
    }

    /**
     * 識別子に該当するエンティティを取得する。
     *
     * @param identity 識別子
     * @return Option[T]
     */
    override def resolveOption(identifier: Identity[JobId]): Option[Job] = {
      jobDao.findById(identifier.value.uuid).flatMap {
        convertInfraToDomain(_).toOption
      }
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
    override def contains(identifier: Identity[JobId]): Try[Boolean] = {
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
    override def store(entity: Job): Try[JobRepository] = {
      Try {
        jobDao.save(entity.identity.uuid, entity.project.identity.uuid, entity.user.identity.value.uuid, entity.exitCode,
          entity.log, entity.executeTimePoint.asJavaUtilDate, entity.executeDuration.quantity)
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
    override def delete(identity: Identity[JobId]): Try[JobRepository] = {
      Try {
        if (jobDao.delete(identity.value.uuid) == 0) {
          throw new EntityNotFoundException
        }
        this
      }
    }

    protected[this] def convertInfraToDomain(infraJob: InfraJob): Try[Job] = {
      projectRepository.resolve(Identity(ProjectId(infraJob.projectId))).flatMap { project =>
        userRepository.resolve(Identity(UserId(infraJob.userId))).map { user =>
          Job(JobId(infraJob.id), project, user, infraJob.exitCode, infraJob.log,
            TimePoint.from(infraJob.executeDate), Duration.milliseconds(infraJob.executeDuration))
        }
      }
    }
  }
}
