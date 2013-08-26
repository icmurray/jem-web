package controllers

import scala.concurrent.future

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current

import domain.{Gateway, Device, Table, Register}
import domain.{DeviceConfig, GatewayConfig, SystemConfig}
import service.SystemService

case class GatewayConfiguration(gateways: List[Gateway])

trait Configuration extends Controller
                    with ControllerUtilities {

  val systemConfigForm = Form(
    mapping(
      "gateways" -> list(
        mapping(
          "host"    -> text,
          "port"    -> number,
          "label"   -> optional(text),
          "devices" -> list(
            mapping(
              "unit"       -> number,
              "type"       -> text,
              "label"      -> optional(text),
              "maxCurrent" -> number,
              "maxVoltage" -> number,
              "maxPower"   -> number
            )(DeviceConfig.apply)(DeviceConfig.unapply)
          )
        )(GatewayConfig.apply)(GatewayConfig.unapply)
      )
    )(SystemConfig.apply)(SystemConfig.unapply)
  )

  val attachedDevicesForm = Form(
    mapping(
      "gateways" -> list(
        mapping(
          "host"    -> text,
          "port"    -> number,
          "label"   -> optional(text),
          "devices" -> list(
            mapping(
              "unit"    -> number,
              "type"    -> text,
              "label"   -> optional(text),
              "tables"  -> list(
                mapping(
                  "id"        -> number,
                  "label"     -> optional(text),
                  "registers" -> list(
                    mapping(
                      "address"           -> number,
                      "label"             -> optional(text),
                      "min"               -> number,
                      "max"               -> optional(number),
                      "unitOfMeasurement" -> optional(text)
                    )(Register.apply)(Register.unapply)
                  )
                )(Table.apply)(Table.unapply)
              )
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
        val gatewayConfigs = gateways.map(GatewayConfig.apply)
        val form = systemConfigForm.fill(SystemConfig(gatewayConfigs))
        Ok(views.html.configuration(form))
      } recover {
        case t => backendIsDownResponse
      }
    }
  }

  def update = Action { implicit request =>
    Async {
      systemConfigForm.bindFromRequest.fold(

        formErrors => future {
          BadRequest(views.html.configuration(formErrors))
        },

        formData   => {

          systemService.attachedDevices.map { gateways =>
            gateways.zip(formData.gateways).map { case (gw, gwConfig) =>
              gwConfig.updateGateway(gw)
            }
          }.flatMap { gateways =>
            systemService.updateAttachedDevices(gateways).map { _ =>
              Redirect(routes.Configuration.index).flashing (
                "success" -> "Successfully updated configuration."
              )
            }
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
