package net.mtgto.carpenter.infrastructure.vcs

import java.util.UUID
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

class DatabaseSnapshotDao extends SnapshotDao {
  import play.api.Play.current

//  protected[this] def convertRowToSnapshot(row: Row): Snapshot = {
//    row match {
//      case Row(name: String, revision: String, branchType: String) =>
//        Snapshot(name = name, revision = revision, branchType = branchType)
//    }
//  }

  override def findByJobId(id: String): Option[Snapshot] = {
    DB.withSession { implicit session =>
      val query = for {
        snapshot <- Snapshots if snapshot.jobId === id
      } yield snapshot
      query.firstOption
    }
//    DB.withConnection{ implicit c =>
//      SQL("SELECT `name`, `revision`, `branch_type` FROM `snapshots` WHERE `job_id` = {id}")
//        .on('id -> id.toString)()
//        .headOption.map(convertRowToSnapshot)
//    }
  }

  override def save(jobId: String, name: String, revision: String, branchType: String) {
    DB.withTransaction { implicit session: Session =>
      val rowCount = Snapshots.where(_.jobId === jobId)
        .map(s => s.name ~ s.revision ~ s.branchType)
        .update((name, revision, branchType))
      if (rowCount == 0) {
        Snapshots.insert(Snapshot(jobId, name, revision, branchType))
      }
    }
//    DB.withConnection{ implicit c =>
//      val rowCount =
//        SQL("UPDATE `snapshots` SET `name` = {name}, `revision` = {revision}, `branch_type` = {branchType} WHERE `job_id` = {jobId}")
//          .on('jobId -> jobId.toString, 'name -> name, 'revision -> revision, 'branchType -> branchType).executeUpdate()
//      if (rowCount == 0)
//        SQL("INSERT INTO `snapshots` (`job_id`, `name`, `revision`, `branch_type`) VALUES ({jobId},{name},{revision},{branchType})")
//          .on('jobId -> jobId.toString, 'name -> name, 'revision -> revision, 'branchType -> branchType).executeInsert()
//    }
  }
}
