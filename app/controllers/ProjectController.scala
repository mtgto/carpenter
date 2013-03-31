package net.mtgto.controllers

import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import java.util.UUID
import net.mtgto.domain.{User, UserRepository, Project, ProjectFactory, ProjectRepository}
import scala.util.{Try, Success, Failure}
import scalaz.Identity

object ProjectController extends Controller with Secured {
  protected[this] val userRepository: UserRepository = UserRepository()

  protected[this] val projectRepository: ProjectRepository = ProjectRepository()

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

  def showCreateView = IsAuthenticated { user => implicit request =>
    Ok(views.html.projects.create(createForm))
  }

  def create = IsAuthenticated { user => implicit request =>
    createForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.projects.create(formWithErrors)).flashing("error" -> "Inputs has someshing wrong"),
      success => {
        success match {
          case (name, hostname, recipe) =>
            val project = ProjectFactory(name, hostname, recipe)
            projectRepository.store(project)
            Redirect(routes.Application.index).flashing("success" -> "Successed to create a project!")
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
        Redirect(routes.Application.index).flashing("error" -> "Not found project to edit")
    }
  }

  def edit(id: String) = IsAuthenticated { user => implicit request =>
    editForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.projects.edit(id, formWithErrors)).flashing("error" -> "Inputs has someshing wrong"),
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
            Redirect(routes.Application.index).flashing("error" -> "Not found project to edit")
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