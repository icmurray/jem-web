package controllers

import java.nio.ByteBuffer

import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current


import service.SystemService

trait SystemController extends Controller
                       with ControllerUtilities {
  def systemService: SystemService

  def start = Action {
    Async {
      systemService.start.map { _ =>
        Redirect(routes.Application.index)
      } recover {
        case t => backendIsDownResponse
      }
    }
  }

  def stop = Action {
    Async {
      systemService.stop.map { _ =>
        Redirect(routes.Application.index)
      } recover {
        case t => backendIsDownResponse
      }
    }
  }
}

object System extends SystemController {
  override val systemService = SystemService 
}
