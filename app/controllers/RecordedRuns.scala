package controllers

import scala.concurrent.future

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current

import domain.{RecordedRunConfiguration, ConfiguredDevice, ConfiguredGateway}
import service.SystemService

trait RecordedRuns extends Controller
                   with ControllerUtilities {

  val createRunForm = Form(
    mapping(
      "selections" -> list(
        mapping(
          "host"    -> text,
          "port"    -> number,
          "devices" -> list(
            mapping(
              "unit"   -> number,
              "table1" -> boolean,
              "table2" -> boolean,
              "table3" -> boolean,
              "table4" -> boolean,
              "table5" -> boolean,
              "table6" -> boolean
            )(ConfiguredDevice.apply)(ConfiguredDevice.unapply)
          )
        )(ConfiguredGateway.apply)(ConfiguredGateway.unapply)
      )
    )(RecordedRunConfiguration.apply)(RecordedRunConfiguration.unapply)
  )

  def systemService: SystemService

  private def gatewaysF = systemService.attachedDevices
  private def recordingsF = systemService.recordedRuns

  def index = Action { implicit request =>
    Async {

      gatewaysF.zip(recordingsF).map { case (gateways, recordings) =>
        val form = createRunForm.fill(
          RecordedRunConfiguration(
            gateways.map(gateway => ConfiguredGateway(
              host=gateway.host,
              port=gateway.port,
              devices=gateway.devices.map { device =>
                ConfiguredDevice(device.unit)
              }
            ))
          )
        )
        Ok(views.html.recordedRuns(form, recordings))
      } recover {
        case t => backendIsDownResponse
      }
    }
  }

  def create = Action { implicit request =>
    Async {
      createRunForm.bindFromRequest.fold(
        formErrors     => recordingsF.map { recordings =>
          BadRequest(views.html.recordedRuns(formErrors, recordings))
        },

        configuration  => {
          systemService.startRecordedRun(configuration).map { _ =>
            Redirect(routes.RecordedRuns.index).flashing(
              "success" -> ("Successfully started new recording.")
            )
          } recover {
            case t => backendIsDownResponse
          }
        }
      )
    }
  }

  def stop(id: String) = Action { implicit request =>
    Async {
      systemService.stopRecordedRun(id).map { _ =>
        Redirect(routes.RecordedRuns.index).flashing(
          "success" -> ("Successfully stopped recording.")
        )
      } recover {
        case service.NotFound => NotFound(s"Unknown Recording: ${id}")
        case _                => backendIsDownResponse
      }
    }
  }
}

object RecordedRuns extends RecordedRuns {
  override val systemService = SystemService 
}
