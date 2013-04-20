package net.mtgto.carpenter.domain

import java.net.URI
import net.mtgto.carpenter.infrastructure.{SourceRepository => InfraSourceRepository, SourceRepositoryDao, DatabaseSourceRepositoryDao}

trait SourceRepositoryService {
  def get(projectId: ProjectId): SourceRepository
  def save(projectId: ProjectId, sourceRepository: SourceRepository): Unit
  def delete(projectId: ProjectId): Boolean
  def resolveSourceRepositoryType(str: String): SourceRepositoryType.Value
  def resolveURIByBranch(sourceRepository: SourceRepository, branchType: BranchType.Value, branchName: String): URI
}

object SourceRepositoryService extends SourceRepositoryService {
  private val sourceRepositoryDao: SourceRepositoryDao = new DatabaseSourceRepositoryDao

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
