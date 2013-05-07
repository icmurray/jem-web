package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current

import service.SystemService

case class Gateway(host: String, port: Int)
case class Device(unit: Int, gateway: Gateway)
case class TableSelection(
  device: Device,
  table1: Boolean,
  table2: Boolean,
  table3: Boolean,
  table4: Boolean,
  table5: Boolean,
  table6: Boolean)

trait RecordedRuns extends Controller
                             with ControllerUtilities {

  val createRunForm = Form(
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
    )(TableSelection.apply)(TableSelection.unapply)
  )

  def systemService: SystemService

  def index = Action { implicit request =>
    Ok(views.html.recordedRuns(createRunForm))
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
