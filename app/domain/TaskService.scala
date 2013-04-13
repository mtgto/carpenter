package net.mtgto.carpenter.domain

import org.sisioh.baseunits.scala.time.{Duration, TimePoint}
import org.sisioh.baseunits.scala.timeutil.Clock
import scala.concurrent.{Future, future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process.{Process, ProcessLogger}

trait TaskService {
  def getAllTasks(project: Project): Seq[Task]
  def execute(project: Project, taskName: String): Future[(Int, String, TimePoint, Duration)]
  def getAllBranches(project: Project): Future[Seq[String]]
  def getAllTags(project: Project): Future[Seq[String]]
}

class DefaultTaskService(workspacePath: String) extends TaskService {
  protected[this] def getProjectWorkspace(project: Project): java.io.File = {
    new java.io.File(workspacePath, project.identity.value.toString)
  }

  protected[this] def createProjectWorkspace(project: Project): Unit = {
    val projectDirectory = getProjectWorkspace(project)
    if (!projectDirectory.isDirectory) {
      projectDirectory.mkdirs
    }
    val file = new java.io.File(projectDirectory, "Capfile")
    val writer = new java.io.PrintWriter(file)
    writer.print(project.recipe)
    writer.close
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

  override def execute(project: Project, taskName: String): Future[(Int, String, TimePoint, Duration)] = {
    createProjectWorkspace(project)
    future {
      val outputBuilder = new StringBuilder
      val errorBuilder = new StringBuilder
      val startTimePoint = Clock.now
      val process: Process = Process(Seq("cap", taskName, "HOSTS="+project.hostname), getProjectWorkspace(project)).run(
        ProcessLogger(
          line => {
            outputBuilder ++= line
            outputBuilder ++= System.lineSeparator
          }, line => {
            errorBuilder ++= line
            errorBuilder ++= System.lineSeparator
          }))
      val exitCode = process.exitValue
      val executeDuration = Duration.milliseconds(Clock.now.breachEncapsulationOfMillisecondsFromEpoc - startTimePoint.breachEncapsulationOfMillisecondsFromEpoc)
      (exitCode, errorBuilder.toString, startTimePoint, executeDuration)
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
}