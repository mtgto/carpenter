package net.mtgto.carpenter.domain

import java.util.UUID
import org.sisioh.dddbase.core.model.{Identity, Entity}
import net.mtgto.carpenter.domain.vcs.SourceRepository

case class ProjectId(uuid: UUID) extends Identity[UUID] {
  override def value = uuid
}

/**
 * Project
 *
 * @param identity identity
 * @param name name
 * @param hostname hostname
 * @param recipe recipe
 */
trait Project extends Entity[ProjectId] {
  override val identity: ProjectId
  val name: String
  val hostname: String
  val sourceRepository: SourceRepository
  val recipe: String

  override def toString: String = Seq(identity, name, hostname, sourceRepository, recipe).mkString("Project(", ", ",")")
}

object Project {
  private case class DefaultProject(identity: ProjectId,
                                    name: String,
                                    hostname: String,
                                    sourceRepository: SourceRepository,
                                    recipe: String) extends Project

  def apply(identity: ProjectId,
            name: String,
            hostname: String,
            sourceRepository: SourceRepository,
            recipe: String): Project = {
    DefaultProject(identity, name, hostname, sourceRepository, recipe)
  }
}
