package net.mtgto.carpenter.infrastructure.vcs

import scala.concurrent.{ExecutionContext, future, Future}
import scala.sys.process.Process
import java.net.URI
import scala.xml.{NodeSeq, XML}
import org.sisioh.baseunits.scala.time.TimePoint
import ExecutionContext.Implicits.global
import javax.xml.bind.DatatypeConverter

class SubversionService extends VCSService {
  private val command = "svn"

  def getRevisions(uri: URI): Future[Seq[SubversionRevision]] = {
    future {
      // ProcessBuilder#!! throws an Exception when exit code != 0
      val xml = XML.load(Process(Seq(command, "ls", "--xml", uri.toString)).!!)
      convertNodeSeqToSubversionRevisions(xml)
    }
  }

  def convertNodeSeqToSubversionRevisions(nodeSeq: NodeSeq): Seq[SubversionRevision] = {
    nodeSeq \ "list" \ "entry" map { entry =>
      SubversionRevision(
        (entry \ "name").text,
        (entry \ "commit").head.attribute("revision").mkString.toLong,
        (entry \ "commit" \ "author").text,
        TimePoint.from(DatatypeConverter.parseDateTime((entry \ "commit" \ "date").text))
      )
    }
  }
}
