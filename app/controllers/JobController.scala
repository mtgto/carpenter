package net.mtgto.carpenter.controllers

import java.util.UUID
import net.mtgto.carpenter.domain.{LogBroadcaster, UserRepository, JobRepository, JobId, Job}
import org.sisioh.dddbase.core.EntityNotFoundException
import play.api.mvc._
import play.api.i18n.Messages
import play.api.libs.iteratee.{Enumerator, Input, Done}
import play.api.libs.json.JsValue
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object JobController extends Controller with Secured {
  protected[this] val userRepository: UserRepository = UserRepository()

  protected[this] val jobRepository: JobRepository = JobRepository()

  def log(id: String) = IsAuthenticatedWS { user => implicit request =>
    val iteratee = Done[JsValue, Unit]((),Input.EOF)
    getJobByIdString(id) match {
      case Some(job) =>
        val enumerator: Enumerator[JsValue] = LogBroadcaster.subscribe(job.identity).getOrElse(Enumerator.eof[JsValue])
        Future((iteratee, enumerator))
      case _ =>
        val enumerator = Enumerator.eof[JsValue]
        Future((iteratee, enumerator))
    }
  }

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
