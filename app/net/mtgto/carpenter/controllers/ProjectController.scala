package net.mtgto.carpenter.controllers

import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.libs.json._

import java.net.URI
import java.util.UUID
import net.mtgto.carpenter.domain._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}
import org.sisioh.baseunits.scala.timeutil.Clock

object ProjectController extends Controller with BaseController {
  protected[this] val userRepository: UserRepository = UserRepository()

  protected[this] val projectRepository: ProjectRepository = ProjectRepository()

  protected[this] val jobRepository: JobRepository = JobRepository()

  private val workspacePath = getConfiguration("carpenter.workspace")

  protected[this] val sourceRepositoryService: SourceRepositoryService = SourceRepositoryService

  protected[this] val taskService = new net.mtgto.carpenter.domain.DefaultTaskService(workspacePath)

  private val sourceRepositoryTypes = Seq("git" -> "Git", "subversion" -> "Subversion")

  private val notification = Notification(
    getConfiguration("carpenter.irc.hostname"),
    getConfiguration("carpenter.irc.port").toInt,
    getConfiguration("carpenter.irc.username"),
    getConfiguration("carpenter.irc.channel_name"),
    getConfiguration("carpenter.irc.encoding"))

  protected[this] val createForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "hostname" -> nonEmptyText,
      "recipe" -> nonEmptyText,
      "sourceRepositoryType" -> nonEmptyText,
      "url" -> nonEmptyText
    )
  )

  protected[this] val editForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "hostname" -> nonEmptyText,
      "recipe" -> nonEmptyText,
      "sourceRepositoryType" -> nonEmptyText,
      "url" -> nonEmptyText
    )
  )

  protected[this] val executeForm = Form(
    tuple(
      "branchType" -> nonEmptyText,
      "branchName" -> text
    )
  )

  def showCreateView = IsAuthenticated { user => implicit request =>
    Ok(views.html.projects.create(createForm, sourceRepositoryTypes))
  }

  def create = IsAuthenticated { user => implicit request =>
    createForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.projects.create(formWithErrors, sourceRepositoryTypes))
          .flashing("error" -> Messages("messages.wrong_input")),
      success => {
        success match {
          case (name, hostname, recipe, sourceRepositoryTypeString, url) =>
            val sourceRepositoryType = sourceRepositoryService.resolveSourceRepositoryType(sourceRepositoryTypeString)
            val sourceRepository = SourceRepository(sourceRepositoryType, new URI(url))
            val project = ProjectFactory(name, hostname, sourceRepository, recipe)
            projectRepository.store(project)
            Redirect(routes.Application.index).flashing("success" -> Messages("messages.create_project"))
        }
      }
    )
  }

  def showEditView(id: String) = IsAuthenticated { user => implicit request =>
    getProjectByIdString(id) match {
      case Success(project) =>
        Ok(views.html.projects.edit(id, editForm.fill(
          (project.name, project.hostname, project.recipe, project.sourceRepository.sourceRepositoryType.toString,
            project.sourceRepository.uri.toString)), sourceRepositoryTypes))
      case _ =>
        Redirect(routes.Application.index).flashing("error" -> Messages("messages.not_found_project_to_edit"))
    }
  }

  def edit(id: String) = IsAuthenticated { user => implicit request =>
    editForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.projects.edit(id, formWithErrors, sourceRepositoryTypes)).flashing("error" -> Messages("messages.wrong_input")),
      success => {
        getProjectByIdString(id) match {
          case Success(project) => {
            success match {
              case (name, hostname, recipe, sourceRepositoryTypeString, url) =>
                val sourceRepositoryType = sourceRepositoryService.resolveSourceRepositoryType(sourceRepositoryTypeString)
                val sourceRepository = SourceRepository(sourceRepositoryType, new URI(url))
                projectRepository.store(
                  Project(project.identity, name, hostname, sourceRepository, recipe))
                Redirect(routes.Application.index).flashing("success" -> Messages("messages.edit_project"))
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
      case Success(project) =>
        val jobs = jobRepository.findAllByProjectOrderByTimePointDesc(project).get
        Ok(views.html.projects.index(project, jobs))
      case _ =>
        Redirect(routes.Application.index).flashing("error" -> Messages("messages.not_found_project_to_edit"))
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
      case Success(project) => {
        val tasks = taskService.getAllTasks(project)
        Ok(Json.obj("status" -> "ok", "tasks" -> Json.toJson(tasks)))
      }
      case _ =>
        BadRequest(Json.obj("status" -> "fail"))
    }
  }

  def showExecuteTaskView(id: String, taskName: String) = IsAuthenticated { user => implicit request =>
    getProjectByIdString(id) match {
      case Success(project) =>
        Async {
          val branchAndTags = for {
            branches <- taskService.getAllBranches(project)
            tags <- taskService.getAllTags(project)
          } yield (branches, tags)
          branchAndTags.map( result => result match {
            case (branches, tags) => Ok(views.html.projects.execute(project, taskName, branches, tags))
          })
        }
      case _ =>
        BadRequest("")
    }
  }

  def executeTask(id: String, taskName: String) = IsAuthenticated { user => implicit request =>
    executeForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("status" -> "fail")),
      success => success match {
        case (branchTypeString, branchName) => {
          getProjectByIdString(id) match {
            case Success(project) =>
              val branchType = branchTypeString match {
                case "branch" => BranchType.Branch
                case "tag" => BranchType.Tag
                case "trunk" => BranchType.Trunk
              }
              val snapshot = sourceRepositoryService.resolveSnapshot(project.sourceRepository, branchType, branchName).get
              val repositoryUri = sourceRepositoryService.resolveURIByBranch(project.sourceRepository, branchType, branchName)
              val currentTimePoint = Clock.now
              val job = JobFactory(project, user, snapshot, taskName, currentTimePoint)
              jobRepository.store(job)
              LogBroadcastService.start(job.identity)
              taskService.execute(job, project, taskName, repositoryUri, branchType, branchName).map { result =>
                result match {
                  case (exitCode, log, executeTimePoint, executeDuration) => {
                    val executedJob = JobFactory(job, exitCode, log, executeDuration)
                    jobRepository.store(executedJob)
                    LogBroadcastService.broadcast(executedJob, log)
                    LogBroadcastService.stop(job.identity)
                    val message = if (executedJob.isSuccess)
                      Messages("messages.notification.success", user.name, project.name, taskName)
                    else
                      Messages("messages.notification.failure", user.name, project.name, taskName)
                    NotificationService.notify(notification, message)
                  }
                }
              }
              Redirect(routes.JobController.showJobView(job.identity.uuid.toString))
            case Failure(_) =>
              Redirect(routes.Application.index).flashing("error" -> Messages("messages.not_found_project_to_execute"))
          }
        }
      }
    )
  }

  def branches(id: String) = IsAuthenticated { user => implicit request =>
    getProjectByIdString(id) match {
      case Success(project) => {
        Async {
          taskService.getAllBranches(project).map { branches =>
            Ok(Json.obj("status" -> "ok", "branches" -> Json.toJson(branches)))
          }
        }
      }
      case _ =>
        BadRequest(Json.obj("status" -> "fail"))
    }
  }

  def tags(id: String) = IsAuthenticated { user => implicit request =>
    getProjectByIdString(id) match {
      case Success(project) => {
        Async {
          taskService.getAllTags(project).map { tags =>
            Ok(Json.obj("status" -> "ok", "tags" -> Json.toJson(tags)))
          }
        }
      }
      case _ =>
        BadRequest(Json.obj("status" -> "fail"))
    }
  }

  protected[this] def getProjectByIdString(id: String): Try[Project] = {
    Try(UUID.fromString(id)).flatMap(uuid => projectRepository.resolve(ProjectId(uuid)))
  }
}