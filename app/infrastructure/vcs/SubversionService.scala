package net.mtgto.carpenter.infrastructure.vcs

import scala.sys.process.Process
import java.net.URI
import scala.xml.{NodeSeq, XML}
import org.sisioh.baseunits.scala.time.TimePoint
import javax.xml.bind.DatatypeConverter
import scala.util.Try

class SubversionService extends VCSService {
  private val command = "svn"

  def getRevisions(uri: URI): Try[Seq[SubversionRevision]] = {
    Try {
      // ProcessBuilder#!! throws an Exception when exit code != 0
      val xml = XML.loadString(Process(Seq(command, "ls", "--xml", uri.toString)).!!)
      convertListNodeSeqToSubversionRevisions(xml)
    }
  }

  def getRevision(uri: URI): Try[SubversionRevision] = {
    Try {
      // ProcessBuilder#!! throws an Exception when exit code != 0
      val xml = XML.loadString(Process(Seq(command, "info", "--xml", uri.toString)).!!)
      convertInfoNodeSeqToSubversionRevision(xml)
    }
  }

  def convertListNodeSeqToSubversionRevisions(nodeSeq: NodeSeq): Seq[SubversionRevision] = {
    nodeSeq \ "list" \ "entry" map { entry =>
      SubversionRevision(
        (entry \ "name").text,
        (entry \ "commit").head.attribute("revision").mkString.toLong,
        (entry \ "commit" \ "author").text,
        TimePoint.from(DatatypeConverter.parseDateTime((entry \ "commit" \ "date").text))
      )
    }
  }

  def convertInfoNodeSeqToSubversionRevision(nodeSeq: NodeSeq): SubversionRevision = {
    val entry = (nodeSeq \ "entry").head
    SubversionRevision(
      entry.attribute("path").get.text,
      entry.attribute("revision").get.text.toLong,
      (entry \ "author").text,
      TimePoint.from(DatatypeConverter.parseDateTime((entry \ "commit" \ "date").text))
    )
  }
}
