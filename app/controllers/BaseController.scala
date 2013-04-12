package net.mtgto.carpenter.controllers

import play.api.Play
import play.api.mvc.Controller

trait BaseController extends Secured {
  self: Controller =>

  protected[this] def getConfiguration(name: String): String = Play.current.configuration.getString(name).get
}
