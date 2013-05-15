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
  def updateAttachedDevices(gateways: List[Gateway])(implicit ec: ExecutionContext): Future[Unit]
  def startRecordedRun(config: RecordedRunConfiguration)(implicit ec: ExecutionContext): Future[Unit]
  def recordedRuns(implicit ec: ExecutionContext): Future[List[Recording]]
  def stopRecordedRun(id: String)(implicit ec: ExecutionContext): Future[Unit]
  def recordingDetail(id: String)(implicit ec: ExecutionContext): Future[Option[Recording]]
  def currentRecording(implicit ec: ExecutionContext): Future[Option[Recording]]
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
    (__ \ "type").read[String] ~
    (__ \ "label").read[Option[String]]
  )(Device)

  implicit private val gatewayReads: Reads[Gateway] = (
    (__ \ "host").read[String] ~
    (__ \ "port").read[Int] ~
    (__ \ "label").read[Option[String]] ~
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

  implicit private val systemStatusReads: Reads[SystemStatus] = (
    (__ \ "running").read[Boolean] ~
    (__ \ "active_recordings").read[List[String]]
  )(SystemStatus)

  implicit private val deviceWrites: Writes[Device] = (
    (__ \ "unit").write[Int] ~
    (__ \ "type").write[String] ~
    (__ \ "label").write[Option[String]]
  )(unlift(Device.unapply))

  implicit private val gatewayWrites: Writes[Gateway] = (
    (__ \ "host").write[String] ~
    (__ \ "port").write[Int] ~
    (__ \ "label").write[Option[String]] ~
    (__ \ "devices").write[List[Device]]
  )(unlift(Gateway.unapply))

  implicit private val configuredDeviceWrites: Writes[ConfiguredDevice] = (
    (__ \ "unit").write[Int] ~
    (__ \ "table_ids").write[List[Int]]
  )(d => (
    d.unit,
    List(d.table1, d.table2, d.table3,
         d.table4, d.table5, d.table6).zipWithIndex.filter(_._1).map(_._2+1)
  ))

  implicit private val configuredGatewayWrites: Writes[ConfiguredGateway] = (
    (__ \ "host").write[String] ~
    (__ \ "port").write[Int] ~
    (__ \ "configured_devices").write[List[ConfiguredDevice]]
  )(unlift(ConfiguredGateway.unapply))

  override def recordedRuns(implicit ec: ExecutionContext) = {
    WS.url(recordingsUrl).get().map { response =>
      val aggregate = response.json.validate[RecordingAggregate]
      aggregate.fold(
        errors => throw new RuntimeException("Bad response"),
        valid  => valid.recordings
      )
    }
  }

  /** TODO: this should call the API for an *individual* recording */
  override def recordingDetail(id: String)(implicit ec: ExecutionContext) = {
    recordedRuns.map { recordings =>
      recordings.find(_.id == id)
    }
  }

  override def currentRecording(implicit ec: ExecutionContext) = {
    status.flatMap { status =>
      val recordingId = status.activeRecordingIds.headOption
      if (! recordingId.isDefined) {
        future { None }
      } else {
        recordingDetail(recordingId.get)
      }
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


  override def updateAttachedDevices(gateways: List[Gateway])(implicit ec: ExecutionContext) = {
    val data = Json.toJson(gateways)
    WS.url(devicesUrl).put(data).map {
      case response if response.status == 200 => {}
      case r                                  => throw new RuntimeException()
    }
  }

  override def startRecordedRun(config: RecordedRunConfiguration)
                               (implicit ec: ExecutionContext) = {

    val data = Json.toJson(config.selections)
    WS.url(recordingsUrl).post(data).map {
      case response if response.status == 201 => {}
      case response if response.status == 409 => throw SystemConflict
      case _                                  => throw new RuntimeException()
    }

  }

  def status(implicit ec: ExecutionContext) = {
    WS.url(statusUrl).get().map { response =>
      response.json.validate[SystemStatus].fold(
        errors => throw new RuntimeException("Bad Response"),
        valid  => valid
      )
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
