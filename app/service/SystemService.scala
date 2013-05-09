package service

import scala.concurrent.{Future, ExecutionContext, future}

import com.typesafe.config._

import org.joda.time.DateTime

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS

import domain.{SystemStatus, Device, Gateway, RecordedRunConfiguration, Recording, ConfiguredGateway, ConfiguredDevice}

trait SystemService {
  def status(implicit ec: ExecutionContext): Future[SystemStatus]
  def start(implicit ec: ExecutionContext): Future[Unit]
  def stop(implicit ec: ExecutionContext): Future[Unit]
  def attachedDevices(implicit ec: ExecutionContext): Future[List[Gateway]]
  def startRecordedRun(config: RecordedRunConfiguration)(implicit ec: ExecutionContext): Future[Unit]
  def recordedRuns(implicit ec: ExecutionContext): Future[List[Recording]]
  def stopRecordedRun(id: String)(implicit ec: ExecutionContext): Future[Unit]
}

object SystemService extends SystemService {

  val config = ConfigFactory.load()
  val backendUrl = config.getString("backend.url")

  private val statusUrl  = backendUrl + "/system-control/status"
  private val startUrl   = backendUrl + "/system-control/start"
  private val stopUrl    = backendUrl + "/system-control/stop"
  private val devicesUrl = backendUrl + "/system-control/attached-devices"
  private val recordingsUrl = backendUrl + "/system-control/recordings"
  private val stopRecordingUrl = backendUrl + "/system-control/recordings/"

  implicit private val jodaDateReads = new Reads[DateTime] {

    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsNumber(d) => JsSuccess(new DateTime((d * 1000L).toLong))
      case _           => {
        JsError(Seq(JsPath() -> Seq(ValidationError(
          "validate.error.expected.date"))))
      }
    }
  }

  implicit private val deviceReads: Reads[Device] = (
    (__ \ "unit").read[Int] ~
    (__ \ "unit").read[Int]     // Workaround for requiring > 1 field.
  )((unit,_) => Device(unit))

  implicit private val gatewayReads: Reads[Gateway] = (
    (__ \ "host").read[String] ~
    (__ \ "port").read[Int] ~
    (__ \ "devices").read[List[Device]]
  )(Gateway)

  /** Because the API doesn't provide top-level arrays */
  private case class GatewayAggregate(gateways: List[Gateway])
  private case class RecordingAggregate(recordings: List[Recording])

  implicit private val aggGatewayReads: Reads[GatewayAggregate] = (
    (__ \ "gateways").read[List[Gateway]] ~
    (__ \ "gateways").read[List[Gateway]] // Workaround..
  )((gws, _) => GatewayAggregate(gws))

  implicit private val configuredDeviceReads: Reads[ConfiguredDevice] = (
    (__ \ "unit").read[Int] ~
    (__ \ "table_ids").read[List[Int]]
  )((unit, ids) => ConfiguredDevice(
    unit = unit,
    table1 = ids.contains(1),
    table2 = ids.contains(2),
    table3 = ids.contains(3),
    table4 = ids.contains(4),
    table5 = ids.contains(5),
    table6 = ids.contains(6)))

  implicit private val configuredGatewayReads: Reads[ConfiguredGateway] = (
    (__ \ "host").read[String] ~
    (__ \ "port").read[Int] ~
    (__ \ "configured_devices").read[List[ConfiguredDevice]]
  )(ConfiguredGateway)

  implicit private val recordingReads: Reads[Recording] = (
    (__ \ "id").read[String] ~
    (__ \ "status").read[String] ~
    (__ \ "start_time").read[DateTime] ~
    (__ \ "end_time").read[Option[DateTime]] ~
    (__ \ "configured_gateways").read[List[ConfiguredGateway]]
  )(Recording)

  implicit private val aggRecordingReads: Reads[RecordingAggregate] = (
    (__ \ "recordings").read[List[Recording]] ~
    (__ \ "recordings").read[List[Recording]] // Workaround..
  )((rs, _) => RecordingAggregate(rs))

  override def recordedRuns(implicit ec: ExecutionContext) = {
    WS.url(recordingsUrl).get().map { response =>
      val aggregate = response.json.validate[RecordingAggregate]
      aggregate.fold(
        errors => throw new RuntimeException("Bad response"),
        valid  => valid.recordings
      )
    }
  }

  override def stopRecordedRun(id: String)(implicit ec: ExecutionContext) = {
    WS.url(stopRecordingUrl + id + "/stop").put("").map {
      case response if response.status == 200 => {}
      case response if response.status == 404 => throw NotFound
      case _                                  => throw new RuntimeException()
    }
  }

  override def attachedDevices(implicit ec: ExecutionContext) = {
    WS.url(devicesUrl).get().map { response =>
      val aggregate = response.json.validate[GatewayAggregate]
      aggregate.fold(
        errors => throw new RuntimeException("Bad response"),
        valid  => valid.gateways
      )
    }
  }

  override def startRecordedRun(config: RecordedRunConfiguration)
                               (implicit ec: ExecutionContext) = future {

  }

  def status(implicit ec: ExecutionContext) = {
    WS.url(statusUrl).get().map { response =>
      SystemStatus((response.json \ "running").as[Boolean])
    }
  }

  override def start(implicit ec: ExecutionContext) = {
    WS.url(startUrl).post("").map(toUnit)
  }

  override def stop(implicit ec: ExecutionContext) = {
    WS.url(stopUrl).post("").map(toUnit)
  }

  private def toUnit[T](t: T): Unit = { }

}
