package net.mtgto.carpenter.infrastructure

import java.util.{Date, UUID}

trait JobDao {
  def findById(id: UUID): Option[Job]
  def findAll: Seq[Job]
  def findAllByProject(projectId: UUID): Seq[Job]
  def findAllByProjectOrderByDateDesc(projectId: UUID): Seq[Job]
  /**
   * @param executeTime: start time to execute the job
   * @param executeDuration: duration of the job
   */
  def save(id: UUID, projectId: UUID, userId: UUID, exitCode: Int, log: String, executeDate: Date, executeDuration: Long): Unit
  def delete(id: UUID): Int
}
