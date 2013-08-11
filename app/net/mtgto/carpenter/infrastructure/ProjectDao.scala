package net.mtgto.carpenter.infrastructure

import java.util.UUID

trait ProjectDao {
  def findById(id: String): Option[Project]
  def findAll: Seq[Project]
  def save(id: String, name: String, hostname: String, recipe: String): Unit
  def delete(id: String): Int
}
