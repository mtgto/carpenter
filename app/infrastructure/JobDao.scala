package net.mtgto.carpenter.infrastructure

import java.util.{Date, UUID}

trait JobDao {
  def findById(id: UUID): Option[Job]
  def findAll: Seq[Job]
  def findAllByProject(projectId: UUID): Seq[Job]
  def findAllByProjectOrderByDateDesc(projectId: UUID): Seq[Job]
  def save(job: Job): Unit
  def delete(id: UUID): Int
}
