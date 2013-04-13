package net.mtgto.carpenter.domain

import java.util.UUID
import org.sisioh.dddbase.core.Entity
import scalaz.Identity

/**
 * Project
 *
 * @param identity identity
 * @param name name
 * @param hostname hostname
 * @param recipe recipe
 */
trait Project extends Entity[UUID] {
  override val identity: Identity[UUID]
  val name: String
  val hostname: String
  val sourceRepository: SourceRepository
  val recipe: String

  override def toString: String = Seq(identity, name, hostname, sourceRepository, recipe).mkString("Project(", ", ",")")
}

object Project {
  private case class DefaultProject(identity: Identity[UUID],
                                    name: String,
                                    hostname: String,
                                    sourceRepository: SourceRepository,
                                    recipe: String) extends Project

  def apply(identity: Identity[UUID],
            name: String,
            hostname: String,
            sourceRepository: SourceRepository,
            recipe: String): Project = {
    DefaultProject(identity, name, hostname, sourceRepository, recipe)
  }
}
