package controllers

import java.nio.ByteBuffer

import play.api._
import play.api.mvc._
import play.api.Play.current

import service.SystemService

trait SystemController extends Controller {
  def systemService: SystemService

  def start = Action {
    systemService.start()
    Redirect(routes.Application.index)
  }

  def stop = Action {
    systemService.stop()
    Redirect(routes.Application.index)
  }
}

object System extends SystemController {
  override val systemService = SystemService 
}
