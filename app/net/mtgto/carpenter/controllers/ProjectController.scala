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
import net.mtgto.carpenter.domain.vcs._
import org.sisioh.baseunits.scala.timeutil.Clock
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}

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
      "subversionPaths" -> text,
      "url" -> nonEmptyText
    )
  )

  protected[this] val editForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "hostname" -> nonEmptyText,
      "recipe" -> nonEmptyText,
      "sourceRepositoryType" -> nonEmptyText,
      "subversionPaths" -> nonEmptyText,
      "url" -> nonEmptyText
    )
  )

  protected[this] val executeForm = Form(
    tuple(
      "branchType" -> nonEmptyText,
      "branchName" -> optional(text),
      "tagName" -> optional(text)
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
          case (name, hostname, recipe, sourceRepositoryTypeString, subversionPaths, url) =>
            val sourceRepositoryType = sourceRepositoryService.resolveSourceRepositoryType(sourceRepositoryTypeString)
            val sourceRepository = convertToSourceRepository(new URI(url), sourceRepositoryType, subversionPaths)
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
        val subversionPaths = project.sourceRepository match {
          case sourceRepository: SubversionSourceRepository => sourceRepository.getPathsString
          case _: GitSourceRepository => ""
        }
        Ok(views.html.projects.edit(id, editForm.fill(
          (project.name, project.hostname, project.recipe, project.sourceRepository.sourceRepositoryType.toString,
            subversionPaths, project.sourceRepository.uri.toString)), sourceRepositoryTypes))
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
              case (name, hostname, recipe, sourceRepositoryTypeString, subversionPaths, url) =>
                val sourceRepositoryType = sourceRepositoryService.resolveSourceRepositoryType(sourceRepositoryTypeString)
                val sourceRepository = convertToSourceRepository(new URI(url), sourceRepositoryType, subversionPaths)
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
          project.sourceRepository match {
            case sourceRepository: GitSourceRepository => {
              val branchAndTags = for {
                branches <- taskService.getAllBranches(project)
                tags <- taskService.getAllTags(project)
              } yield (branches, tags)
              branchAndTags.map( result => result match {
                case (branches, tags) => Ok(views.html.projects.executeGit(project, taskName, branches, tags))
              })
            }
            case sourceRepository: SubversionSourceRepository => {
              taskService.getAllSubversionBranches(project, sourceRepository.paths.withFilter(_.pathType == SubversionPathType.Parent).map(_.name)).map {
                parents =>
                  val children: Seq[SubversionPath] = sourceRepository.paths.filter(_.pathType == SubversionPathType.Child)
                  Ok(views.html.projects.executeSubversion(project, taskName, parents, children))
              }
            }
          }
        }
      case _ =>
        BadRequest("")
    }
  }

  def executeTask(id: String, taskName: String) = IsAuthenticated { user => implicit request =>
    executeForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("status" -> "fail")),
      success => success match {
        case (branchTypeString, branchName, tagName) => {
          getProjectByIdString(id) match {
            case Success(project) =>
              val (branchType, snapshotBranchName) = (project.sourceRepository.sourceRepositoryType, branchTypeString, branchName) match {
                case (SourceRepositoryType.Git, "branch", _) => (BranchType.Branch, branchName.get)
                case (SourceRepositoryType.Git, "tag", _) => (BranchType.Tag, tagName.get)
                case (SourceRepositoryType.Subversion, _, None) => (BranchType.Branch, branchTypeString)
                case (SourceRepositoryType.Subversion, _, Some(branchName)) => (BranchType.Branch, branchName)
              }
              val snapshot = sourceRepositoryService.resolveSnapshot(project.sourceRepository, branchType, snapshotBranchName).get
              val repositoryUri = sourceRepositoryService.resolveURIByBranch(project.sourceRepository, branchType, snapshotBranchName)
              val currentTimePoint = Clock.now
              val job = JobFactory(project, user, snapshot, taskName, currentTimePoint)
              jobRepository.store(job)
              LogBroadcastService.start(job.identity)
              taskService.execute(job, project, taskName, repositoryUri, branchType, snapshotBranchName).map { result =>
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

  protected[this] def convertToSourceRepository(uri: URI, sourceRepositoryType: SourceRepositoryType.Value, subversionPathsString: String): SourceRepository = {
    sourceRepositoryType match {
      case SourceRepositoryType.Git => GitSourceRepository(uri)
      case SourceRepositoryType.Subversion => SubversionSourceRepository(uri, parseSubversionPathsString(subversionPathsString))
    }
  }

  protected[this] def parseSubversionPathsString(pathsString: String): Seq[SubversionPath] = {
    pathsString.lines.map { line =>
      line.lastOption match {
        case Some('/') => SubversionPath(SubversionPathType.Parent, line.dropRight(1))
        case _ => SubversionPath(SubversionPathType.Child, line)
      }
    }.toSeq
  }
}