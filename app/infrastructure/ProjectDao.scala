package net.mtgto.infrastructure

import java.util.UUID

trait ProjectDao {
  def findById(id: UUID): Option[Project]
  def findAll: Seq[Project]
  def save(id: UUID, name: String, hostname: String, recipe: String): Unit
  def delete(id: UUID): Int
}
