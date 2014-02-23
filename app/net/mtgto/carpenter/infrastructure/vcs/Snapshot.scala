package net.mtgto.carpenter.infrastructure.vcs

import java.util.UUID
import play.api.db.slick.Config.driver.simple._
import net.mtgto.carpenter.infrastructure.Jobs

case class Snapshot(jobId: String, name: String, revision: String, branchType: String)

class Snapshots(tag: Tag) extends Table[Snapshot](tag, "snapshots") {
  def jobId = column[String]("job_id", O.NotNull)
  def name = column[String]("name", O.NotNull)
  def revision = column[String]("revision", O.NotNull)
  def branchType = column[String]("branch_type", O.NotNull)
  def * = (jobId, name, revision, branchType) <> (Snapshot.tupled, Snapshot.unapply)
  def job = foreignKey("job_id", jobId, TableQuery[Jobs])(_.id)
  def idx = index("job_id", jobId, unique = true)
}
