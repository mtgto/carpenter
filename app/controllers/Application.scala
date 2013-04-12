package net.mtgto.carpenter.controllers

import play.api._
import play.api.mvc._

import net.mtgto.carpenter.domain.{UserRepository, ProjectRepository}

object Application extends Controller with BaseController {
  protected[this] val userRepository: UserRepository = UserRepository()

  protected[this] val projectRepository: ProjectRepository = ProjectRepository()
  
  def index = IsAuthenticated { user => implicit request =>
    val projects = projectRepository.findAll
    val users = userRepository.findAll
    Ok(views.html.index(projects, users))
  }
  
}
