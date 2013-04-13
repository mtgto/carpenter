package net.mtgto.carpenter.domain

import java.util.UUID
import scalaz.Identity

trait ProjectFactory {
  def apply(name: String, hostname: String, sourceRepository: SourceRepository, recipe: String): Project
}

object ProjectFactory extends ProjectFactory {
  override def apply(name: String, hostname: String, sourceRepository: SourceRepository, recipe: String): Project = {
    Project(Identity(UUID.randomUUID), name, hostname, sourceRepository, recipe)
  }
}
