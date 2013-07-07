package net.mtgto.carpenter.domain.vcs

import java.net.URI
import net.mtgto.carpenter.domain.{ProjectId}
import net.mtgto.carpenter.infrastructure.vcs.{GitService, GitRevision => InfraGitRevision, SubversionRevision => InfraSubversionRevision, SubversionService, SubversionPath => InfraSubversionPath}
import net.mtgto.carpenter.infrastructure.{SourceRepository => InfraSourceRepository, SourceRepositoryDao, DatabaseSourceRepositoryDao, GitSourceRepository => InfraGitSourceRepository, SubversionSourceRepository => InfraSubversionSourceRepository}
import scala.util.Try
import org.sisioh.dddbase.core.lifecycle.EntityNotFoundException

trait SourceRepositoryService {
  def get(projectId: ProjectId): SourceRepository
  def save(projectId: ProjectId, sourceRepository: SourceRepository): Unit
  def delete(projectId: ProjectId): Boolean
  def resolveSourceRepositoryType(str: String): SourceRepositoryType.Value
  def resolveURIByBranch(sourceRepository: SourceRepository, branchType: BranchType.Value, branchName: String): URI
  def resolveSnapshot(sourceRepository: SourceRepository, branchType: BranchType.Value, branchName: String): Try[Snapshot]
}

object SourceRepositoryService extends SourceRepositoryService {
  private val sourceRepositoryDao: SourceRepositoryDao = new DatabaseSourceRepositoryDao

  private val subversionService: SubversionService = new SubversionService

  private val gitService: GitService = new GitService

  override def get(projectId: ProjectId): SourceRepository = {
    val infraSourceRepository = sourceRepositoryDao.findByProjectId(projectId.uuid)
    infraSourceRepository match {
      case Some(infraSourceRepository: InfraGitSourceRepository) =>
        GitSourceRepository(infraSourceRepository.uri)
      case Some(infraSourceRepository: InfraSubversionSourceRepository) =>
        SubversionSourceRepository(infraSourceRepository.uri, infraSourceRepository.paths.map(convertInfraSubversionPathToDomain))
      case _ => throw new EntityNotFoundException
    }
  }

  override def save(projectId: ProjectId, sourceRepository: SourceRepository): Unit = {
    val infraSourceRepository = sourceRepository match {
      case sourceRepository: GitSourceRepository =>
        InfraGitSourceRepository(sourceRepository.uri)
      case sourceRepository: SubversionSourceRepository =>
        InfraSubversionSourceRepository(sourceRepository.uri, sourceRepository.paths.map(convertSubversionPathToInfra))
    }
    sourceRepositoryDao.save(projectId.uuid, infraSourceRepository)
  }

  override def delete(projectId: ProjectId): Boolean = {
    sourceRepositoryDao.deleteByProjectId(projectId.uuid) == 1
  }

  override def resolveSourceRepositoryType(software: String): SourceRepositoryType.Value = {
    software match {
      case "subversion" => SourceRepositoryType.Subversion
      case "git" => SourceRepositoryType.Git
      case _ => throw new IllegalArgumentException(s"invalid software: $software")
    }
  }

  override def resolveURIByBranch(sourceRepository: SourceRepository, branchType: BranchType.Value, branchName: String): URI = {
    (sourceRepository.sourceRepositoryType, branchType) match {
      case (SourceRepositoryType.Subversion, BranchType.Branch) =>
        new URI(sourceRepository.uri.toString.stripSuffix("/") + "/" + branchName)
      case (SourceRepositoryType.Subversion, BranchType.Tag) =>
        new URI(sourceRepository.uri.toString.stripSuffix("/") + "/tags/" + branchName)
      case (SourceRepositoryType.Git, _) =>
        sourceRepository.uri
    }
  }

  override def resolveSnapshot(sourceRepository: SourceRepository, branchType: BranchType.Value, branchName: String): Try[Snapshot] = {
    val uri = resolveURIByBranch(sourceRepository, branchType, branchName)
    (sourceRepository.sourceRepositoryType, branchType) match {
      case (SourceRepositoryType.Subversion, _) =>
        subversionService.getRevision(uri).map(convertSubversionRevisionToSnapshot(branchName, branchType, uri, _))
      case (SourceRepositoryType.Git, BranchType.Branch) =>
        gitService.getBranchRevision(uri, branchName).map(convertGitRevisionToSnapshot(branchType, _))
      case (SourceRepositoryType.Git, BranchType.Tag) =>
        gitService.getTagRevision(uri, branchName).map(convertGitRevisionToSnapshot(branchType, _))
    }
  }

  private def convertSubversionRevisionToSnapshot(branchName: String, branchType: BranchType.Value, uri: URI, revision: InfraSubversionRevision): Snapshot = {
    SubversionSnapshot(branchName, branchType, uri, SubversionRevision(revision.revision))
  }

  private def convertGitRevisionToSnapshot(branchType: BranchType.Value, revision: InfraGitRevision): Snapshot = {
    branchType match {
      case BranchType.Branch =>
        GitBranchSnapshot(name = revision.name, revision = GitRevision(revision.revision))
      case BranchType.Tag =>
        GitTagSnapshot(name = revision.name, revision = GitRevision(revision.revision))
    }
  }

  private def convertSourceRepositoryTypeToInfra(sourceRepository: SourceRepository): String = {
    sourceRepository.sourceRepositoryType match {
      case SourceRepositoryType.Subversion => "subversion"
      case SourceRepositoryType.Git => "git"
    }
  }

  protected[this] def convertInfraSubversionPathToDomain(path: InfraSubversionPath): SubversionPath = {
    val pathType = if (path.isDirectory) SubversionPathType.Parent else SubversionPathType.Child
    SubversionPath(pathType, path.path)
  }

  protected[this] def convertSubversionPathToInfra(path: SubversionPath): InfraSubversionPath = {
    path.pathType match {
      case SubversionPathType.Parent => InfraSubversionPath(path.name, true)
      case SubversionPathType.Child => InfraSubversionPath(path.name, false)
    }
  }
}
