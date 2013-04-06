package net.mtgto.controllers

import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.libs.json._

import java.util.UUID
import net.mtgto.domain.{User, UserRepository, Project, ProjectFactory, ProjectRepository, JobRepository, JobFactory}
import net.mtgto.domain.Task
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}
import scalaz.Identity

object ProjectController extends Controller with BaseController {
  protected[this] val userRepository: UserRepository = UserRepository()

  protected[this] val projectRepository: ProjectRepository = ProjectRepository()

  protected[this] val jobRepository: JobRepository = JobRepository()

  private val workspacePath = getConfiguration("carpenter.workspace")

  protected[this] val taskService = new net.mtgto.domain.DefaultTaskService(workspacePath)

  protected[this] val createForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "hostname" -> nonEmptyText,
      "recipe" -> nonEmptyText
    )
  )

  protected[this] val editForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "hostname" -> nonEmptyText,
      "recipe" -> nonEmptyText
    )
  )

  protected[this] val taskNameForm = Form(
    "name" -> nonEmptyText
  )

  def showCreateView = IsAuthenticated { user => implicit request =>
    Ok(views.html.projects.create(createForm))
  }

  def create = IsAuthenticated { user => implicit request =>
    createForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.projects.create(formWithErrors)).flashing("error" -> Messages("messages.wrong_input")),
      success => {
        success match {
          case (name, hostname, recipe) =>
            val project = ProjectFactory(name, hostname, recipe)
            projectRepository.store(project)
            Redirect(routes.Application.index).flashing("success" -> Messages("messages.create_project"))
        }
      }
    )
  }

  def showEditView(id: String) = IsAuthenticated { user => implicit request =>
    getProjectByIdString(id) match {
      case Some(project) =>
        Ok(views.html.projects.edit(id, editForm.fill(
          (project.name, project.hostname, project.recipe))))
      case _ =>
        Redirect(routes.Application.index).flashing("error" -> Messages("messages.not_found_project_to_edit"))
    }
  }

  def edit(id: String) = IsAuthenticated { user => implicit request =>
    editForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.projects.edit(id, formWithErrors)).flashing("error" -> Messages("messages.wrong_input")),
      success => {
        getProjectByIdString(id) match {
          case Some(project) => {
            success match {
              case (name, hostname, recipe) =>
                projectRepository.store(
                  Project(project.identity, name, hostname, recipe))
                Redirect(routes.Application.index).flashing("success" -> "Successed to edit a project!")
            }
          }
          case _ =>
            Redirect(routes.Application.index).flashing("error" -> Messages("messages.not_found_project_to_edit"))
        }
      }
    )
  }

  def showProjectView(id: String) = IsAuthenticated { user => implicit request =>
    getProjectByIdString(id) match {
      case Some(project) =>
        Ok(views.html.projects.index(project))
      case _ =>
        Redirect(routes.Application.index).flashing("error" -> "Not found project to edit")
    }
  }

  def tasks(id: String) = IsAuthenticated { user => implicit request =>
    implicit val taskWrites = new Writes[Task] {
      def writes(task: Task): JsValue = {
        Json.obj(
          "name" -> task.name,
          "description" -> task.description
        )
      }
    }
    getProjectByIdString(id) match {
      case Some(project) => {
        val tasks = taskService.getAllTasks(project)
        Logger.info(tasks.toString)
        Ok(Json.obj("status" -> "ok", "tasks" -> Json.toJson(tasks)))
      }
      case _ =>
        BadRequest(Json.obj("status" -> "fail"))
    }
  }

  def executeTask(id: String) = IsAuthenticated { user => implicit request =>
    taskNameForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(Json.obj("status" -> "fail")),
      taskName => {
        getProjectByIdString(id) match {
          case Some(project) =>
            Async {
              taskService.execute(project, taskName).map( result =>
                result match {
                  case (exitCode, log) => {
                    val job = JobFactory(project, exitCode, log)
                    jobRepository.store(job)
                    Ok(Json.obj("status" -> "ok", "task" -> Json.toJson(taskName), "exitCode" -> exitCode, "log" -> log))
                  }
                }
              )
            }
          case None =>
              BadRequest(Json.obj("status" -> "fail"))
        }
      }
    )
  }

  protected[this] def getProjectByIdString(id: String): Option[Project] = {
    Try(UUID.fromString(id)) match {
      case Success(uuid) =>
        projectRepository.resolveOption(Identity(uuid))
      case Failure(e) =>
        None
    }
  }
}