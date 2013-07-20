package net.mtgto.carpenter.domain

import net.mtgto.carpenter.domain.vcs._
import net.mtgto.carpenter.infrastructure.{Job => InfraJob, JobDao, DatabaseJobDao, Project => InfraProject, User => InfraUser, Authority => InfraAuthority}
import net.mtgto.carpenter.infrastructure.vcs.{Snapshot => InfraSnapshot, SnapshotDao, DatabaseSnapshotDao}
import org.sisioh.baseunits.scala.time.{Duration, TimePoint}
import org.sisioh.dddbase.core.lifecycle.{ResultWithEntity, EntityNotFoundException}
import org.sisioh.dddbase.core.lifecycle.sync.{SyncResultWithEntity, SyncRepository}
import scala.util.Try
import net.mtgto.carpenter.domain.vcs.SubversionSnapshot
import net.mtgto.carpenter.domain.vcs.Snapshot
import net.mtgto.carpenter.domain.vcs.GitTagSnapshot
import net.mtgto.carpenter.domain.vcs.GitBranchSnapshot

trait JobRepository extends SyncRepository[JobId, Job] with BaseEntityReader[JobId, Job] {
  def findAll: Try[Seq[Job]]

  def findAllByProject(project: Project): Try[Seq[Job]]

  def findAllByProjectOrderByTimePointDesc(project: Project): Try[Seq[Job]]

  def findAllByUser(user: User): Try[Seq[Job]]
}

object JobRepository {
  def apply(): JobRepository = new JobRepository {
    override type This = JobRepository

    private val jobDao: JobDao = new DatabaseJobDao

    protected[this] val projectRepository: ProjectRepository = ProjectRepository()

    protected[this] val userRepository: UserRepository = UserRepository()

    protected[this] val sourceRepositoryService: SourceRepositoryService = SourceRepositoryService

    protected[this] val snapshotDao: SnapshotDao = new DatabaseSnapshotDao

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

    override def findAllByUser(user: User): Try[Seq[Job]] = {
      Try {
        jobDao.findAllByUser(user.identity.value.uuid).map(convertInfraToDomain(_).get)
      }
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
    override def resolve(identity: JobId): Try[Job] = {
      jobDao.findById(identity.value.uuid).map(convertInfraToDomain).getOrElse(throw new EntityNotFoundException)
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
    def store(entity: Job): Try[ResultWithEntity[This, JobId, Job, Try]] = {
      Try {
        val infraProject = InfraProject(entity.project.identity.uuid, entity.project.name, entity.project.hostname, entity.project.recipe)
        val infraAuthority = InfraAuthority(canLogin = entity.user.authority.canLogin, canCreateUser = entity.user.authority.canCreateUser)
        val infraUser = InfraUser(entity.user.identity.uuid, entity.user.name, infraAuthority)
        jobDao.save(InfraJob(entity.identity.uuid, infraProject, infraUser,
          entity.taskName, entity.exitCode, entity.log, entity.executeTimePoint.asJavaUtilDate, entity.executeDuration.map(_.quantity)))
        val (snapshotName, snapshotRevision, branchType) = entity.snapshot match {
          case snapshot: GitBranchSnapshot => (snapshot.name, snapshot.revision, "branch")
          case snapshot: GitTagSnapshot => (snapshot.name, snapshot.revision, "tag")
          case snapshot: SubversionSnapshot => (snapshot.name, snapshot.revision.revision.toString, snapshot.branchType.toString)
        }
        snapshotDao.save(entity.identity.uuid, name = snapshotName, revision = snapshotRevision.toString, branchType)
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
    override def delete(identity: JobId): Try[This] = {
      Try {
        if (jobDao.delete(identity.value.uuid) == 0) {
          throw new EntityNotFoundException
        }
        this
      }
    }

    protected[this] def convertInfraToDomain(infraJob: InfraJob): Try[Job] = {
      val project = ProjectFactory(infraJob.project)
      val user = UserFactory(infraJob.user)
      convertInfraSnapshotToDomain(project.sourceRepository, snapshotDao.findByJobId(infraJob.id).get).map { snapshot =>
        (infraJob.exitCode, infraJob.log, infraJob.executeDuration) match {
          case (Some(exitCode), Some(log), Some(executeDuration)) =>
            Job(JobId(infraJob.id), project, user, snapshot, infraJob.task, exitCode, log,
              TimePoint.from(infraJob.executeDate), Duration.milliseconds(executeDuration))
          case _ =>
            Job(JobId(infraJob.id), project, user, snapshot, infraJob.task,
              TimePoint.from(infraJob.executeDate))
        }

      }
    }

    protected[this] def convertInfraSnapshotToDomain(sourceRepository: SourceRepository, snapshot: InfraSnapshot): Try[Snapshot] = {
      Try {
        (sourceRepository.sourceRepositoryType, BranchType.withName(snapshot.branchType)) match {
          case (SourceRepositoryType.Git, BranchType.Branch) =>
            GitBranchSnapshot(snapshot.name, GitRevision(snapshot.revision))
          case (SourceRepositoryType.Git, BranchType.Tag) =>
            GitTagSnapshot(snapshot.name, GitRevision(snapshot.revision))
          case (SourceRepositoryType.Subversion, branchType) =>
            val uri = sourceRepositoryService.resolveURIByBranch(sourceRepository, branchType, snapshot.name)
            val revision = SubversionRevision(snapshot.revision.toLong)
            SubversionSnapshot(snapshot.name, branchType, uri, revision)
        }
      }
    }
  }
}
