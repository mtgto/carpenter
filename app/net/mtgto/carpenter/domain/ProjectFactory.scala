package net.mtgto.carpenter.domain

import java.util.UUID
import net.mtgto.carpenter.domain.vcs.SourceRepository

trait ProjectFactory {
  def apply(name: String, hostname: String, sourceRepository: SourceRepository, recipe: String): Project
}

object ProjectFactory extends ProjectFactory {
  override def apply(name: String, hostname: String, sourceRepository: SourceRepository, recipe: String): Project = {
    Project(ProjectId(UUID.randomUUID), name, hostname, sourceRepository, recipe)
  }
}
