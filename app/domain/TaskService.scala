package net.mtgto.domain

import scala.concurrent.{Future, future}
import scala.concurrent.ExecutionContext.Implicits.global

trait TaskService {
  def getAllTasks(project: Project): Seq[Task]

  def execute(project: Project, taskName: String): Future[(Int, String)]
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
    import scala.sys.process.Process
    createProjectWorkspace(project)
    val process: String = Process("cap -vT", getProjectWorkspace(project)).!!
    process.split("\n").withFilter(_.contains("#")).map{ line =>
      val index = line.lastIndexOf("#")
      val (name, description) = line.splitAt(index)
      Task(name.trim.stripPrefix("cap "), description.stripPrefix("#").trim)
    }
  }

  override def execute(project: Project, taskName: String): Future[(Int, String)] = {
    createProjectWorkspace(project)
    future {
      import scala.sys.process.{Process, ProcessLogger}
      val outputBuilder = new StringBuilder
      val errorBuilder = new StringBuilder
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
      (exitCode, errorBuilder.toString)
    }
  }
}