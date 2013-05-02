package net.mtgto.carpenter.domain

import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import scala.collection.mutable.Map
import play.api.libs.json.{Json, JsValue}

object LogBroadcaster {
  private val channels = Map.empty[JobId, (Enumerator[JsValue], Channel[JsValue])]

  def start(jobId: JobId) = {
    channels += jobId -> Concurrent.broadcast
  }

  def stop(jobId: JobId) = {
    channels.remove(jobId).map(_._2.eofAndEnd())
  }

  def subscribe(jobId: JobId): Option[Enumerator[JsValue]] = {
    channels.get(jobId).map(_._1)
  }

  def broadcast(jobId: JobId, message: String) = {
    channels.get(jobId).map(_._2.push(Json.toJson(scala.collection.immutable.Map("log" -> message))))
  }
}
