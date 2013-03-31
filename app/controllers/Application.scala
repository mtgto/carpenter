package net.mtgto.controllers

import play.api._
import play.api.mvc._

import java.util.UUID
import net.mtgto.domain.{User, UserRepository, ProjectRepository}
import scala.util.{Failure, Success, Try}
import scalaz.Identity

object Application extends Controller with Secured {
  protected[this] val userRepository: UserRepository = UserRepository()

  protected[this] val projectRepository: ProjectRepository = ProjectRepository()
  
  def index = IsAuthenticated { user => implicit request =>
    val projects = projectRepository.findAll
    Ok(views.html.index(projects))
  }
  
}

trait Secured {
  protected val userRepository: UserRepository

  protected def getUser(request: RequestHeader): Option[User] = {
    request.session.get("userId").flatMap( userId =>
      Try(UUID.fromString(userId)) match {
        case Success(id) => userRepository.resolveOption(Identity(id))
        case Failure(e) => None
      }
    )
  }

  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.UserController.login)

  def IsAuthenticated(f: => User => Request[AnyContent] => Result) = Security.Authenticated(getUser, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }

  def IsAuthenticated[A](bodyParser: BodyParser[A])(f: => User => Request[A] => Result) = Security.Authenticated(getUser, onUnauthorized) { user =>
    Action(bodyParser)(request => f(user)(request))
  }
}