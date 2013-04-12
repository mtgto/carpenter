package net.mtgto.carpenter.infrastructure

import java.util.UUID

trait UserDao {
  def findById(id: UUID): Option[User]
  def findByNameAndPassword(name: String, password: String): Option[User]
  def findAll: Seq[User]
  def save(id: UUID, name: String, password: String, authority: Authority): Unit

  /**
   * return the number of deleted rows
   */
  def delete(id: UUID): Int
}
