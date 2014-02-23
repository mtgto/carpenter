package net.mtgto.carpenter.infrastructure.slick

import java.sql.Timestamp
import org.sisioh.baseunits.scala.time.TimePoint
import play.api.db.slick.Config.driver.simple._

object TimePointMapper {
  implicit def date2dateTime = MappedColumnType.base[TimePoint, Timestamp] (
    timePoint => new Timestamp(timePoint.breachEncapsulationOfMillisecondsFromEpoc),
    timestamp => TimePoint.from(timestamp)
  )
}
