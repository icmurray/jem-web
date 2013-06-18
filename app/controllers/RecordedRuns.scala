package controllers

import scala.concurrent.future

import java.io.DataOutputStream

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current

// Reactive Mongo imports
import reactivemongo.api._
import reactivemongo.bson._
import play.modules.reactivemongo._
import play.modules.reactivemongo.json.collection.JSONCollection

import domain.{RecordedRunConfiguration, ConfiguredDevice, ConfiguredGateway}
import domain.{Device, Gateway, Recording, Table}
import service.{SystemService, SystemConflict}

trait RecordedRuns extends Controller
                   with ControllerUtilities {

  lazy val db = ReactiveMongoPlugin.db

  val createRunForm = Form(
    mapping(
      "selections" -> list(
        mapping(
          "host"    -> text,
          "port"    -> number,
          "devices" -> list(
            mapping(
              "enabled"-> boolean,
              "unit"   -> number,
              "label"  -> optional(text),
              "table1" -> boolean,
              "table2" -> boolean,
              "table3" -> boolean,
              "table4" -> boolean,
              "table5" -> boolean,
              "table6" -> boolean
            )(dataToConfiguredDevice _)(configuredDeviceToData _)
          ),
          "label"   -> optional(text)
        )(ConfiguredGateway.apply)(ConfiguredGateway.unapply)
      )
    )(RecordedRunConfiguration.apply)(RecordedRunConfiguration.unapply)
  )

  private def dataToConfiguredDevice(
    enabled: Boolean, unit: Int, label: Option[String],
    table1: Boolean, table2: Boolean, table3: Boolean,
    table4: Boolean, table5: Boolean, table6: Boolean): ConfiguredDevice = {

    ConfiguredDevice(
      unit,
      enabled && table1, enabled && table2, enabled && table3,
      enabled && table4, enabled && table5, enabled && table6,
      label)
  }

  private def configuredDeviceToData(device: ConfiguredDevice) = {
    Some(true, device.unit, device.label,
     device.table1, device.table2, device.table3,
     device.table3, device.table5, device.table6)
  }

  def systemService: SystemService

  private def gatewaysF = systemService.attachedDevices
  private def recordingsF = systemService.recordedRuns
  private def statusF = systemService.status

  def index = Action { implicit request =>
    Async {

      val response = for {
        gateways     <- gatewaysF
        recordings   <- recordingsF
        systemStatus <- statusF
        
        form = createRunForm.fill(
          RecordedRunConfiguration(
            gateways.map(gateway => ConfiguredGateway(
              host=gateway.host,
              port=gateway.port,
              devices=gateway.devices.map { device =>
                ConfiguredDevice(
                  unit=device.unit,
                  table1=true, table2=true, table3=false,
                  table4=false, table5=true, table6=false,
                  label=device.label)
              },
              label=gateway.label
            ))
          )
        )
      } yield Ok(views.html.recordedRuns(form, recordings, systemStatus))

      response.recover { case t => backendIsDownResponse }
    }
  }

  def create = Action { implicit request =>
    Async {
      createRunForm.bindFromRequest.fold(
        formErrors     => recordingsF.zip(statusF).map { case (recordings, systemStatus) =>
          BadRequest(views.html.recordedRuns(
            formErrors, recordings, systemStatus))
        },

        configuration  => {
          systemService.startRecordedRun(configuration).map { _ =>
            Redirect(routes.RecordedRuns.index).flashing(
              "success" -> ("Successfully started new recording.")
            )
          } recover {
            case SystemConflict => {
              Redirect(routes.RecordedRuns.index).flashing(
                "error" -> "Cannot start new recording whilst another one is running."
              )
            }
            case t => backendIsDownResponse
          }
        }
      )
    }
  }

  def details(id: String) = Action { implicit request =>
    Async {
      systemService.recordingDetail(id).map {
        case None    => NotFound(s"Unknown Recording: ${id}")
        case Some(r) => Ok(views.html.recordedRunDetail(r))
      }
    }
  }

  def archive(id: String,
              host: String,
              port: Int,
              unit: Int,
              table: Int, dummy: String = ":") = Action { implicit request =>
    Async {
      systemService.recordingDetail(id).map { oRec =>
        for {
          rec <- oRec
          gw  <- rec.gateways.find(gw => gw.host == host && gw.port == port)
          device <- gw.devices.find(_.unit == unit)
          table <- device.tables.find(_.id == table)
        } yield (rec, gw, device, table)
      }.map {
        case None               => NotFound("")
        case Some((rec, gw, device, table)) => Ok.stream(
          archivedData(rec, gw, device, table) >>> Enumerator.eof
        ).withHeaders(
          "Content-Type"->"application/csv",
          "Content-Disposition"->"attachment; filename=test.csv"
        )
      }
    }
  }

  private def archivedData(rec: Recording, gw: Gateway, device: Device, table: Table) = {

    val registers = table.registers.sortBy(_.address)
    val columnToRegisterAddress = registers.map(_.address).zipWithIndex.map(_.swap).toMap
    val header = csvRow("timestamp" :: registers.map(_.address.toString))

    val query = Json.obj(
      "device.gateway.host" -> gw.host,
      "device.gateway.port" -> gw.port,
      "device.unit"         -> device.unit,
      "table_id"            -> table.id
    )

    val projection = Json.obj(
      "values" -> 1,
      "timing_info.end" -> 1,
      "_id" -> 0
    )

    val collection = db.collection[JSONCollection](s"archive-${rec.id}")
    val cursor = collection.find(query, projection).cursor[JsValue]
    Enumerator(header) >>> cursor.enumerate.map { js =>

      val timestamp = (js \ "timing_info" \ "end").as[Double]
      val values = (js \ "values").as[List[List[Int]]]
                                  .map((pair: List[Int]) => (pair(0), pair(1)))
                                  .toMap
      val cells = (0 until registers.length).map { idx =>
        val addr: Int = columnToRegisterAddress(idx)
        values.get(addr).map(_.toString).getOrElse("")
      }

      csvRow(timestamp.toString :: cells.toList)
    }
  }

  private def archivedData(rec: Recording, gw: Gateway, device: Device) = {

    val registers = (for {
      table <- device.tables
      register <- table.registers
    } yield register).sortBy(_.address)
      
    val columns = registers.map(_.address).zipWithIndex.map(_.swap).toMap
    println(columns)

    val header = "\"timestamp\"," ++ registers.map(_.address.toString).mkString(",") ++ "\n"

    val query = Json.obj(
      "device.gateway.host" -> gw.host,
      "device.gateway.port" -> gw.port,
      "device.unit"         -> device.unit
    )

    val projection = Json.obj(
      "values" -> 1,
      "timing_info.end" -> 1,
      "_id" -> 0
    )

    val collection = db.collection[JSONCollection](s"archive-${rec.id}")
    val cursor = collection.find(query, projection).cursor[JsValue]
    Enumerator(header) >>> cursor.enumerate.map { js =>

      val timestamp = (js \ "timing_info" \ "end").as[Double]
      val values = (js \ "values").as[List[List[Int]]]
                                  .map((pair: List[Int]) => (pair(0), pair(1)))
                                  .toMap
      val cells = (0 until registers.length).map { idx =>
        val addr: Int = columns(idx)
        values.get(addr).map(_.toString).getOrElse("")
      }
      (List(timestamp.toString) ++ cells).mkString(",") + "\n"
    }

    //Enumerator.outputStream { os =>
    //  val out = new DataOutputStream(os)

    //  val registers = for {
    //    table <- device.tables
    //    register <- table.registers
    //  } yield register

    //  val heading = csvRow(registers.map(_.friendlyName))
    //  out.writeChars(heading)

    //  out.close()
    //}
  }

  private def csvRow(ss: Seq[String]) = {
    ss.map(csvColumn _).mkString(",") + "\n"
  }

  private def csvColumn(s: String) = {
    val v = s.replaceAll("\"", "\\\"")
    "\"" + v + "\""
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
