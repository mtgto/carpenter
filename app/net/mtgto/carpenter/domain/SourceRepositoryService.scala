package net.mtgto.carpenter.domain

import java.net.URI
import net.mtgto.carpenter.infrastructure.{SourceRepository => InfraSourceRepository, SourceRepositoryDao, DatabaseSourceRepositoryDao}
import net.mtgto.carpenter.domain.vcs.{GitTagSnapshot, GitBranchSnapshot, SubversionSnapshot, Snapshot}
import net.mtgto.carpenter.infrastructure.vcs.{GitRevision, GitService, SubversionRevision, SubversionService}
import scala.util.Try

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
    val infraSourceRepository = sourceRepositoryDao.findByProjectId(projectId.uuid).head
    val sourceRepositoryType = resolveInfraSourceRepositoryType(infraSourceRepository)
    SourceRepository(sourceRepositoryType, infraSourceRepository.uri)
  }

  override def save(projectId: ProjectId, sourceRepository: SourceRepository): Unit = {
    val infraSourceRepository = InfraSourceRepository(convertSourceRepositoryTypeToInfra(sourceRepository), sourceRepository.uri)
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
        new URI(sourceRepository.uri.toString.stripSuffix("/") + "/branches/" + branchName)
      case (SourceRepositoryType.Subversion, BranchType.Tag) =>
        new URI(sourceRepository.uri.toString.stripSuffix("/") + "/tags/" + branchName)
      case (SourceRepositoryType.Subversion, BranchType.Trunk) =>
        new URI(sourceRepository.uri.toString.stripSuffix("/") + "/trunk")
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

  private def convertSubversionRevisionToSnapshot(branchName: String, branchType: BranchType.Value, uri: URI, revision: SubversionRevision): Snapshot = {
    SubversionSnapshot(branchName, branchType, uri, revision.revision)
  }

  private def convertGitRevisionToSnapshot(branchType: BranchType.Value, revision: GitRevision): Snapshot = {
    branchType match {
      case BranchType.Branch =>
        GitBranchSnapshot(name = revision.name, revision = revision.revision)
      case BranchType.Tag =>
        GitTagSnapshot(name = revision.name, revision = revision.revision)
    }
  }

  private def resolveInfraSourceRepositoryType(infraSourceRepository: InfraSourceRepository): SourceRepositoryType.Value = {
    resolveSourceRepositoryType(infraSourceRepository.software)
  }

  private def convertSourceRepositoryTypeToInfra(sourceRepository: SourceRepository): String = {
    sourceRepository.sourceRepositoryType match {
      case SourceRepositoryType.Subversion => "subversion"
      case SourceRepositoryType.Git => "git"
    }
  }
}
