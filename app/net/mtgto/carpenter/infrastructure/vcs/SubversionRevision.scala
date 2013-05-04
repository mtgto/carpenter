package net.mtgto.carpenter.infrastructure.vcs

import org.sisioh.baseunits.scala.time.TimePoint

case class SubversionRevision(
  name: String,
  revision: Long,
  author: String,
  date: TimePoint
)
