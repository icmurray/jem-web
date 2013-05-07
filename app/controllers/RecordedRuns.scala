package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current

import domain.{Device, Gateway}
import service.SystemService

case class ConfiguredDevice(
  device: Device,
  table1: Boolean = true,
  table2: Boolean = true,
  table3: Boolean = true,
  table4: Boolean = true,
  table5: Boolean = true,
  table6: Boolean = true)

case class RecordedRunConfiguration(
  selections: List[ConfiguredDevice]
)

trait RecordedRuns extends Controller
                             with ControllerUtilities {

  val createRunForm = Form(
    mapping(
      "selections" -> list(
        mapping(
          "device" -> mapping(
            "unit" -> number,
            "gateway" -> mapping(
              "host" -> text,
              "port" -> number
            )(Gateway.apply)(Gateway.unapply)
          )(Device.apply)(Device.unapply),
          "table1" -> boolean,
          "table2" -> boolean,
          "table3" -> boolean,
          "table4" -> boolean,
          "table5" -> boolean,
          "table6" -> boolean
        )(ConfiguredDevice.apply)(ConfiguredDevice.unapply)
      )
    )(RecordedRunConfiguration.apply)(RecordedRunConfiguration.unapply)
  )

  def systemService: SystemService

  def index = Action { implicit request =>
    Async {
      systemService.attachedDevices.map { devices =>
        val form = createRunForm.fill(
          RecordedRunConfiguration(devices.map(device => ConfiguredDevice(device=device)))
        )
        Ok(views.html.recordedRuns(form))
      }
    }
  }

  def create = Action { implicit request =>
    createRunForm.bindFromRequest.fold(
      errors => BadRequest(views.html.recordedRuns(errors)),
      value  => {
        Redirect(routes.RecordedRuns.index).flashing(
          "success" -> ("Successfully created: " + value.toString)
        )
      }
    )
  }
}

object RecordedRuns extends RecordedRuns {
  override val systemService = SystemService 
}
