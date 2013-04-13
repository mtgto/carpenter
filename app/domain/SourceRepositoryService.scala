package net.mtgto.carpenter.domain

import java.util.UUID
import net.mtgto.carpenter.infrastructure.{SourceRepository => InfraSourceRepository, SourceRepositoryDao, DatabaseSourceRepositoryDao}
import scalaz.Identity

trait SourceRepositoryService {
  def get(projectId: Identity[UUID]): SourceRepository
  def save(projectId: Identity[UUID], sourceRepository: SourceRepository): Unit
  def delete(projectId: Identity[UUID]): Boolean
  def resolveSourceRepositoryType(str: String): SourceRepositoryType.Value
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
