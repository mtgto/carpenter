package net.mtgto.infrastructure

import java.util.UUID

trait JobDao {
  def findById(id: UUID): Option[Job]
  def findAll: Seq[Job]
  def save(id: UUID, projectId: UUID, exitCode: Option[Int], log: String): Unit
  def delete(id: UUID): Int
}
