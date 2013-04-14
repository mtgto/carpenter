package net.mtgto.carpenter.domain

import java.net.URI
import java.util.UUID
import net.mtgto.carpenter.infrastructure.{SourceRepository => InfraSourceRepository, SourceRepositoryDao, DatabaseSourceRepositoryDao}
import scalaz.Identity

trait SourceRepositoryService {
  def get(projectId: Identity[UUID]): SourceRepository
  def save(projectId: Identity[UUID], sourceRepository: SourceRepository): Unit
  def delete(projectId: Identity[UUID]): Boolean
  def resolveSourceRepositoryType(str: String): SourceRepositoryType.Value
  def resolveURIByBranch(sourceRepository: SourceRepository, branchType: BranchType.Value, branchName: String): URI
}

object SourceRepositoryService extends SourceRepositoryService {
  private val sourceRepositoryDao: SourceRepositoryDao = new DatabaseSourceRepositoryDao

  override def get(projectId: Identity[UUID]): SourceRepository = {
    val infraSourceRepository = sourceRepositoryDao.findByProjectId(projectId.value).head
    val sourceRepositoryType = resolveInfraSourceRepositoryType(infraSourceRepository)
    SourceRepository(sourceRepositoryType, infraSourceRepository.uri)
  }

  override def save(projectId: Identity[UUID], sourceRepository: SourceRepository): Unit = {
    val infraSourceRepository = InfraSourceRepository(convertSourceRepositoryTypeToInfra(sourceRepository), sourceRepository.uri)
    sourceRepositoryDao.save(projectId.value, infraSourceRepository)
  }

  override def delete(projectId: Identity[UUID]): Boolean = {
    sourceRepositoryDao.deleteByProjectId(projectId.value) == 1
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
