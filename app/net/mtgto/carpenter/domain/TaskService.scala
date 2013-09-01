package net.mtgto.carpenter.domain

import java.net.URI
import org.sisioh.baseunits.scala.time.{Duration, TimePoint}
import org.sisioh.baseunits.scala.timeutil.Clock
import scala.concurrent.{Future, future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process.{Process, ProcessLogger}
import net.mtgto.carpenter.domain.vcs.{SubversionPathType, SubversionPath, BranchType, SourceRepositoryType}

trait TaskService {
  def getAllTasks(project: Project): Seq[Task]
  def execute(job: Job, project: Project, taskName: String, repositoryUri: URI, branchType: BranchType.Value, branchName: String): Future[(Int, String, TimePoint, Duration)]
  def getAllBranches(project: Project): Future[Seq[String]]
  def getAllTags(project: Project): Future[Seq[String]]
  def getAllSubversionBranches(project: Project, parents: Seq[String]): Future[Map[String, Seq[SubversionPath]]]
}

class DefaultTaskService(workspacePath: String) extends TaskService {
  protected[this] def getProjectWorkspace(project: Project): java.io.File = {
    new java.io.File(workspacePath, project.identity.value.uuid.toString)
  }

  protected[this] def createProjectWorkspace(project: Project): Unit = {
    val projectDirectory = getProjectWorkspace(project)
    if (!projectDirectory.isDirectory) {
      projectDirectory.mkdirs
    }
    val file = new java.io.File(projectDirectory, "Capfile")
    val writer = new java.io.PrintWriter(file)
    writer.print(project.recipe)
    writer.close()
  }

  override def getAllTasks(project: Project): Seq[Task] = {
    createProjectWorkspace(project)
    val process: String = Process("cap -vT", getProjectWorkspace(project)).!!
    process.split("\n").withFilter(_.contains("#")).map{ line =>
      val index = line.lastIndexOf("#")
      val (name, description) = line.splitAt(index)
      Task(name.trim.stripPrefix("cap "), description.stripPrefix("#").trim)
    }
  }

  override def execute(job: Job, project: Project, taskName: String, repositoryUri: URI, branchType: BranchType.Value, branchName: String): Future[(Int, String, TimePoint, Duration)] = {
    createProjectWorkspace(project)
    future {
      val outputBuilder = new StringBuilder
      val errorBuilder = new StringBuilder
      val startTimePoint = Clock.now
      val repositoryParams = project.sourceRepository.sourceRepositoryType match {
        case SourceRepositoryType.Subversion =>
          Seq("--set", "scm=subversion", "--set", s"repository=${repositoryUri.toString}")
        case SourceRepositoryType.Git =>
          Seq("--set", "scm=git", "--set", s"repository=${repositoryUri.toString}", "--set", s"branch=$branchName")
      }
      val process: Process = Process(Seq("cap", taskName, "HOSTS="+project.hostname) ++ repositoryParams, getProjectWorkspace(project)).run(
        ProcessLogger(
          line => {
            outputBuilder ++= line
            outputBuilder ++= System.lineSeparator
          }, line => {
            errorBuilder ++= line
            errorBuilder ++= System.lineSeparator
            LogBroadcastService.broadcast(job, errorBuilder.toString())
          }))
      val exitCode = process.exitValue()
      val executeDuration = Duration.milliseconds(Clock.now.breachEncapsulationOfMillisecondsFromEpoc - startTimePoint.breachEncapsulationOfMillisecondsFromEpoc)
      (exitCode, errorBuilder.toString(), startTimePoint, executeDuration)
    }
  }

  override def getAllBranches(project: Project): Future[Seq[String]] = {
    future {
      project.sourceRepository.sourceRepositoryType match {
        case SourceRepositoryType.Subversion =>
          val branchesUri = project.sourceRepository.uri.toString.stripSuffix("/") + "/branches"
          Process(Seq("svn", "ls", branchesUri)).lines.map {
            line => line.stripSuffix("/")
          }
        case SourceRepositoryType.Git =>
          Process(Seq("git", "ls-remote", "--heads", project.sourceRepository.uri.toString)).lines.map {
            line => line.split("""\s+""")(1).stripPrefix("refs/heads/")
          }
      }
    }
  }

  override def getAllTags(project: Project): Future[Seq[String]] = {
    future {
      project.sourceRepository.sourceRepositoryType match {
        case SourceRepositoryType.Subversion =>
          val tagsUri = project.sourceRepository.uri.toString.stripSuffix("/") + "/tags"
          Process(Seq("svn", "ls", tagsUri)).lines.map {
            line => line.stripSuffix("/")
          }
        case SourceRepositoryType.Git =>
          Process(Seq("git", "ls-remote", "--tags", project.sourceRepository.uri.toString)).lines.map {
            line => line.split("""\s+""")(1).stripPrefix("refs/tags/")
          }
      }
    }
  }

  override def getAllSubversionBranches(project: Project, parents: Seq[String]): Future[Map[String, Seq[SubversionPath]]] = {
    future {
      parents.map { parent =>
        val branchUri = project.sourceRepository.uri.toString.stripSuffix("/") + "/" + parent
        parent -> Process(Seq("svn", "ls", branchUri)).lines.map( fileName =>
          SubversionPath(SubversionPathType.Parent, parent + "/" + fileName.stripSuffix("/")))
      }.toMap
    }
  }
}