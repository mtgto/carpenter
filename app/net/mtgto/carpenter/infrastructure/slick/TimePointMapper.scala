package net.mtgto.carpenter.infrastructure.slick

import java.sql.Timestamp
import org.sisioh.baseunits.scala.time.TimePoint
import scala.slick.lifted.MappedTypeMapper

object TimePointMapper {
  implicit def date2dateTime = MappedTypeMapper.base[TimePoint, Timestamp] (
    timePoint => new Timestamp(timePoint.breachEncapsulationOfMillisecondsFromEpoc),
    timestamp => TimePoint.from(timestamp)
  )
}
