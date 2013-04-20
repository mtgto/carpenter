package net.mtgto.carpenter.infrastructure.vcs

import java.util.UUID
import play.api.db.DB
import anorm._

class DatabaseSnapshotDao extends SnapshotDao {
  import play.api.Play.current

  protected[this] def convertRowToSnapshot(row: Row): Snapshot = {
    row match {
      case Row(name: String, revision: String, branchType: String) =>
        Snapshot(name = name, revision = revision, branchType = branchType)
    }
  }

  override def findByJobId(id: UUID): Option[Snapshot] = {
    DB.withConnection{ implicit c =>
      SQL("SELECT `name`, `revision`, `branch_type` FROM `snapshots` WHERE `job_id` = {id}")
        .on('id -> id.toString)()
        .headOption.map(convertRowToSnapshot)
    }
  }

  override def save(jobId: UUID, name: String, revision: String, branchType: String) {
    DB.withConnection{ implicit c =>
      val rowCount =
        SQL("UPDATE `snapshots` SET `name` = {name}, `revision` = {revision}, `branch_type` = {branchType} WHERE `job_id` = {jobId}")
          .on('jobId -> jobId.toString, 'name -> name, 'revision -> revision, 'branchType -> branchType).executeUpdate()
      if (rowCount == 0)
        SQL("INSERT INTO `snapshots` (`job_id`, `name`, `revision`, `branch_type`) VALUES ({jobId},{name},{revision},{branchType})")
          .on('jobId -> jobId.toString, 'name -> name, 'revision -> revision, 'branchType -> branchType).executeInsert()
    }
  }
}
