package net.mtgto.carpenter.infrastructure

import java.util.{Date, UUID}

trait JobDao {
  def findById(id: String): Option[(Job, Project, (User, Authority))]
  def findAll: Seq[(Job, Project, (User, Authority))]
  def findAllByProject(projectId: String): Seq[(Job, Project, (User, Authority))]
  def findAllByProjectOrderByDateDesc(projectId: String): Seq[(Job, Project, (User, Authority))]
  def findAllByUser(userId: String): Seq[(Job, Project, (User, Authority))]
  def save(job: Job): Unit
  def delete(id: String): Int
}
