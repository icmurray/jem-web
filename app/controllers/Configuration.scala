package controllers

import scala.concurrent.future

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current

import domain.{Gateway, Device}
import service.SystemService

case class GatewayConfiguration(gateways: List[Gateway])

trait Configuration extends Controller
                    with ControllerUtilities {

  val attachedDevicesForm = Form(
    mapping(
      "gateways" -> list(
        mapping(
          "host"    -> text,
          "port"    -> number,
          "label"   -> optional(text),
          "devices" -> list(
            mapping(
              "unit"  -> number,
              "type"  -> text,
              "label" -> optional(text)
            )(Device.apply)(Device.unapply)
          )
        )(Gateway.apply)(Gateway.unapply)
      )
    )(GatewayConfiguration.apply)(GatewayConfiguration.unapply)
  )

  def systemService: SystemService

  def index = Action { implicit request =>
    Async {
      systemService.attachedDevices.map { gateways =>
        val form = attachedDevicesForm.fill(GatewayConfiguration(gateways))
        Ok(views.html.configuration(form))
      } recover {
        case t => backendIsDownResponse
      }
    }
  }

  def update = Action { implicit request =>
    Async {
      attachedDevicesForm.bindFromRequest.fold(
        formErrors => future {
          BadRequest(views.html.configuration(formErrors))
        },
        formData   => {
          systemService.updateAttachedDevices(formData.gateways).map { _ =>
            Redirect(routes.Configuration.index).flashing (
              "success" -> "Successfully updated configuration."
            )
          } recover {
            case t => backendIsDownResponse
          }
        }
      )
    }
  }

}

object Configuration extends Configuration {
  override val systemService = SystemService 
}
