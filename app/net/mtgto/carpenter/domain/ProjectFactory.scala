package net.mtgto.carpenter.domain

import java.util.UUID
import net.mtgto.carpenter.domain.vcs.{SourceRepositoryService, SourceRepository}
import net.mtgto.carpenter.infrastructure.{Project => InfraProject}

trait ProjectFactory {
  def apply(name: String, hostname: String, sourceRepository: SourceRepository, recipe: String): Project

  def apply(infraProject: InfraProject): Project
}

object ProjectFactory extends ProjectFactory {
  override def apply(name: String, hostname: String, sourceRepository: SourceRepository, recipe: String): Project = {
    Project(ProjectId(UUID.randomUUID), name, hostname, sourceRepository, recipe)
  }

  override def apply(infraProject: InfraProject): Project = {
    Project(
      ProjectId(infraProject.id), infraProject.name, infraProject.hostname,
      SourceRepositoryService.get(ProjectId(infraProject.id)), infraProject.recipe)
  }
}
