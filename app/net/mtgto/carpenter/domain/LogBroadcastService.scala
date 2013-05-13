package net.mtgto.carpenter.domain

import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Concurrent, Enumerator}
import play.api.libs.json.{JsString, JsBoolean, Json, JsValue}
import scala.collection.mutable.{Map => MutableMap}

object LogBroadcastService {
  private val channels = MutableMap.empty[JobId, (Enumerator[JsValue], Channel[JsValue])]

  def start(jobId: JobId) = {
    channels += jobId -> Concurrent.broadcast
  }

  def stop(jobId: JobId) = {
    channels.remove(jobId).map(_._2.eofAndEnd())
  }

  def subscribe(jobId: JobId): Option[Enumerator[JsValue]] = {
    channels.get(jobId).map(_._1)
  }

  def broadcast(job: Job, message: String) = {
    channels.get(job.identity).map(_._2.push(
      Json.toJson(
        Map(
          "log" -> JsString(message),
          "running" -> JsBoolean(job.isRunning),
          "success" -> JsBoolean(job.isSuccess)))))
  }
}
