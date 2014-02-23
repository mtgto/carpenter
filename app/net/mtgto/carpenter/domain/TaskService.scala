package net.mtgto.carpenter.domain

import java.io.File
import java.net.URI
import org.sisioh.baseunits.scala.time.{Duration, TimePoint}
import org.sisioh.baseunits.scala.timeutil.Clock
import scala.concurrent.{Future, future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process.{Process, ProcessLogger}
import net.mtgto.carpenter.domain.vcs.{SubversionPathType, SubversionPath, BranchType, SourceRepositoryType}

trait TaskService {
  /**
   * Retrieve all tasks of project.
   *
   * @param project the target to retrieve tasks
   * @return When succeeded to retrieve project tasks, returns it.
   *         Or else, return exit code and error message.
   */
  def getAllTasks(project: Project): Future[Seq[Task]]
  def execute(job: Job, project: Project, taskName: String, repositoryUri: URI, branchType: BranchType.Value, branchName: String): Future[(Int, String, TimePoint, Duration)]
  def getAllBranches(project: Project): Future[Seq[String]]
  def getAllTags(project: Project): Future[Seq[String]]
  def getAllSubversionBranches(project: Project, parents: Seq[String]): Future[Map[String, Seq[SubversionPath]]]
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
    createCapfile(new File(projectDirectory, "Capfile"), project)
    createGemfile(new File(projectDirectory, "Gemfile"))
  }

  protected[this] def createCapfile(file: File, project: Project): Unit = {
    val writer = new java.io.PrintWriter(file)
    writer.print(project.recipe)
    writer.close()
  }

  protected[this] def createGemfile(file: File): Unit = {
    val writer = new java.io.PrintWriter(file)
    writer.println("source \"https://rubygems.org/\"")
    writer.println("gem 'capistrano', '~> 2.15.5'")
    writer.close()
  }

  /**
   * Execute the external command and return the tuple of exit code, standard output and standard error.
   */
  protected[this] def executeProcessBuilder(processBuilder: scala.sys.process.ProcessBuilder): (Int, String, String) = {
    val outputBuilder = new StringBuilder
    val errorBuilder = new StringBuilder
    val process = processBuilder.run(ProcessLogger(
      line => {
        outputBuilder ++= line + System.lineSeparator
      }, line => {
        errorBuilder ++= line + System.lineSeparator
      })
    )
    (process.exitValue(), outputBuilder.toString(), errorBuilder.toString())
  }

  override def getAllTasks(project: Project): Future[Seq[Task]] = {
    future {
      createProjectWorkspace(project)
      val (exitCode, output, error) = executeProcessBuilder(Process(s"bundle exec cap -vT", getProjectWorkspace(project)))
      if (exitCode == 0) {
        output.split(System.lineSeparator).withFilter(_.contains("#")).map{ line =>
          val index = line.lastIndexOf("#")
          val (name, description) = line.splitAt(index)
          Task(name.trim.stripPrefix("cap "), description.stripPrefix("#").trim)
        }
      } else {
        throw new RuntimeException(s"Nonzero exit value: $exitCode, error: $error")
      }
    }
  }

  override def execute(job: Job, project: Project, taskName: String, repositoryUri: URI, branchType: BranchType.Value, branchName: String): Future[(Int, String, TimePoint, Duration)] = {
    createProjectWorkspace(project)
    future {
      val startTimePoint = Clock.now
      val repositoryParams = project.sourceRepository.sourceRepositoryType match {
        case SourceRepositoryType.Subversion =>
          Seq("--set", "scm=subversion", "--set", s"repository=${repositoryUri.toString}")
        case SourceRepositoryType.Git =>
          Seq("--set", "scm=git", "--set", s"repository=${repositoryUri.toString}", "--set", s"branch=$branchName")
      }
      val (exitCode, _, errorOutput) =
        executeProcessBuilder(
          Process(Seq("bundle", "exec", "cap", taskName, "HOSTS="+project.hostname) ++ repositoryParams, getProjectWorkspace(project)))
      val executeDuration = Duration.milliseconds(Clock.now.breachEncapsulationOfMillisecondsFromEpoc - startTimePoint.breachEncapsulationOfMillisecondsFromEpoc)
      (exitCode, errorOutput, startTimePoint, executeDuration)
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