package net.mtgto.carpenter.controllers

import play.api._
import play.api.mvc._
import play.api.i18n.Messages
import net.mtgto.carpenter.domain.{UserRepository, JobRepository, JobId, Job}
import scala.util.{Failure, Success, Try}
import java.util.UUID
import org.sisioh.dddbase.core.{EntityNotFoundException, Identity}

object JobController extends Controller with Secured {
  protected[this] val userRepository: UserRepository = UserRepository()

  protected[this] val jobRepository: JobRepository = JobRepository()

  def showJobView(id: String) = IsAuthenticated { user => implicit request =>
    getJobByIdString(id) match {
      case Some(job) =>
        Ok(views.html.jobs.index(job))
      case _ =>
        Redirect(routes.Application.index).flashing("error" -> Messages("messages.not_found_job"))
    }
  }

  protected[this] def getJobByIdString(id: String): Option[Job] = {
    Try(UUID.fromString(id)) match {
      case Success(uuid) =>
        jobRepository.resolveOption(JobId(uuid)).getOrElse(throw new EntityNotFoundException)
      case Failure(e) =>
        None
    }
  }
}
