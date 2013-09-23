package net.mtgto.carpenter.controllers

import java.util.UUID
import net.mtgto.carpenter.domain.{UserId, User, UserRepository}
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc._
import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Secured {
  protected val userRepository: UserRepository

  protected def getUser(request: RequestHeader): Option[User] = {
    request.session.get("userId").flatMap( userId =>
      Try(UUID.fromString(userId)).flatMap(uuid => userRepository.resolve(UserId(uuid))).toOption
    )
  }

  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.UserController.login)

  def IsAuthenticated(f: => User => Request[AnyContent] => SimpleResult) = Security.Authenticated(getUser, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }

  def IsAuthenticatedAsync(f: => User => Request[AnyContent] => Future[SimpleResult]) = Security.Authenticated(getUser, onUnauthorized) { user =>
    Action.async(request => f(user)(request))
  }

  def IsAuthenticatedWS[A](f: => User => RequestHeader => Future[(Iteratee[A, _], Enumerator[A])])(implicit frameFormatter: FrameFormatter[A]) = {
    WebSocket.async { requestHeader =>
      getUser(requestHeader) match {
        case Some(user) => f(user)(requestHeader)
        case _ =>
          val iteratee = Iteratee.skipToEof[A]
          val enumerator =  Enumerator.eof[A]
          Future((iteratee, enumerator))
      }
    }
  }
}
