package net.mtgto.carpenter.infrastructure

import java.util.UUID

trait UserDao {
  def findById(id: String): Option[(User, Authority)]
  def findByNameAndPassword(name: String, password: String): Option[(User, Authority)]
  def findAll: Seq[(User, Authority)]
  def save(id: String, name: String, password: String, authority: Authority): Unit

  /**
   * return the number of deleted rows
   */
  def delete(id: String): Int
}
