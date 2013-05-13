package net.mtgto.carpenter.controllers

import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages

import net.mtgto.carpenter.domain._
import scala.util.{Failure, Success, Try}
import java.util.UUID
import net.mtgto.carpenter.domain.Authority
import scala.Some

object UserController extends Controller with BaseController {
  protected[this] val userRepository: UserRepository = UserRepository()

  protected[this] val jobRepository: JobRepository = JobRepository()

  private val (adminName, adminPassword) = (getConfiguration("carpenter.admin_name"), getConfiguration("carpenter.admin_password"))

  private val passwordHashingSalt = getConfiguration("carpenter.password_salt")

  protected[this] val loginForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "password" -> text
    )
  )

  protected[this] val createForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "password" -> text,
      "canCreateUser" -> boolean
    )
  )

  protected[this] val changePasswordForm = Form(
    tuple(
      "oldPassword" -> text,
      "newPassword" -> text
    )
  )

  protected[this] def getHashedPassword(name: String, password: String): String = {
    val passwordBytes = (name + passwordHashingSalt + password).getBytes("UTF-8")
    toHashedString(passwordBytes)
  }

  protected[this] def toHashedString(bytes: Array[Byte]): String = {
    import java.security.MessageDigest
    val md = MessageDigest.getInstance("SHA-256")
    md.update(bytes)
    md.digest.map("%02x".format(_)).mkString
  }

  def login = Action { implicit request =>
    Ok(views.html.users.login(loginForm))
  }

  def logout = IsAuthenticated { user => implicit request =>
    Redirect(routes.Application.index).withNewSession
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.users.login(formWithErrors)),
      userNameAndPassword => {
        val user: Option[User] = userNameAndPassword match {
          case (name, password) => {
            val hashedPassword = getHashedPassword(name, password)
            userRepository.findByNameAndPassword(name, hashedPassword).orElse {
              if (name == adminName && password == adminPassword) {
                val adminUser = UserFactory.createUser(
                  name, hashedPassword, Authority(canLogin = true, canCreateUser = true))
                userRepository.store(adminUser)
                userRepository.findByNameAndPassword(name, hashedPassword)
              } else {
                None
              }
            }
          }
        }
        user match {
          case Some(user) => 
            Redirect(routes.Application.index).withSession("userId" -> user.identity.value.uuid.toString)
          case None =>
            Redirect(routes.UserController.login).flashing("error" -> Messages("messages.wrong_name_or_password"))
        }
      }
    )
  }

  // TODO check user authority
  def showCreateView = IsAuthenticated { user => implicit request =>
    Ok(views.html.users.create(createForm))
  }

  def create = IsAuthenticated { user => implicit request =>
    createForm.bindFromRequest.fold(
      formWithErrors =>
        Redirect(routes.Application.index).flashing("error" -> Messages("messages.wrong_input")),
      success => success match {
        case (name, password, canCreateUser) =>
          userRepository.store(
            UserFactory.createUser(
              name, getHashedPassword(name, password), Authority(canLogin = true, canCreateUser = canCreateUser)))
          Redirect(routes.Application.index).flashing("success" -> Messages("messages.create_user", name))
      }
    )
  }

  def changePassword = IsAuthenticated { user => implicit request =>
    changePasswordForm.bindFromRequest.fold(
      formWithErrors =>
        Redirect(routes.Application.index).flashing("error" -> (Messages("messages.wrong_input"))),
      success => success match {
        case (oldPassword, newPassword) =>
          val oldHashedPassword = getHashedPassword(user.name, oldPassword)
          userRepository.findByNameAndPassword(user.name, oldHashedPassword) match {
            case Some(user) =>
              userRepository.store(User(user.identity.value, user.name, Some(getHashedPassword(user.name, newPassword)), user.authority))
              Redirect(routes.Application.index).flashing("success" -> Messages("messages.users.changed_password"))
            case _ =>
              Redirect(routes.Application.index).flashing("error" -> Messages("messages.wrong_password"))
          }
      }
    )
  }

  def jobs(id: String) = IsAuthenticated { user => implicit request =>
    getUserByIdString(id) match {
      case Success(targetUser) =>
        jobRepository.findAllByUser(targetUser) match {
          case Success(jobs) =>
            Ok(views.html.users.jobs(targetUser, jobs))
          case Failure(_) =>
            Redirect(routes.Application.index).flashing("error" -> Messages("messages.not_found_job"))
        }

      case Failure(_) =>
        Redirect(routes.Application.index).flashing("error" -> Messages("messages.not_found_user"))
    }

  }

  protected[this] def getUserByIdString(id: String): Try[User] = {
    Try(UUID.fromString(id)).flatMap(uuid => userRepository.resolve(UserId(uuid)))
  }
}