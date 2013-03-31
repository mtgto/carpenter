package net.mtgto.domain

trait TaskService {
  def getAllTasks(project: Project): Seq[Task]
}

class DefaultTaskService(workspacePath: String) extends TaskService {
  private val descRegex = """""".r
  protected[this] def createProjectWorkspace(project: Project): Unit = {
    val projectDirectory = new java.io.File(workspacePath, project.identity.value.toString)
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
    val process: String = Process("cap -T", new java.io.File(workspacePath)).!!
    process.split("\n").withFilter(_.contains(" # ")).map{ line =>
      val index = line.lastIndexOf(" # ")
      val nameAndDescription = line.split(" # ")
      Task(nameAndDescription(0).trim, nameAndDescription(1).trim)
    }
  }
}