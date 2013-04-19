package net.mtgto.carpenter.infrastructure.vcs

import org.specs2.mutable.Specification
import scala.xml.XML

class SubversionServiceSpec extends Specification {
  "SubversionService" should {
    "convert xml into revisions" in {
      val service = new SubversionService
      val nodeSeq = XML.load(getClass.getResource("/svn-branches.xml"))
      val revisions = service.convertNodeSeqToSubversionRevisions(nodeSeq)
      revisions.size === 3
    }
  }
}
