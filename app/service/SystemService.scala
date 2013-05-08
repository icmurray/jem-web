package service

import scala.concurrent.{Future, ExecutionContext, future}

import com.typesafe.config._

import org.joda.time.DateTime

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS

import domain.{SystemStatus, Device, Gateway, RecordedRunConfiguration, Recording}

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
  private val stopRecordingUrl = backendUrl + "/system-control/recordings/"

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

  implicit private val aggGatewayReads: Reads[GatewayAggregate] = (
    (__ \ "gateways").read[List[Gateway]] ~
    (__ \ "gateways").read[List[Gateway]] // Workaround..
  )((gws, _) => GatewayAggregate(gws))

  override def recordedRuns(implicit ec: ExecutionContext) = future {
    List(
      Recording(
        id="518a6eb6a63728758313e9fd",
        status="running",
        startTime=new DateTime(2013, 5, 8, 16, 41, 22),
        endTime=None,
        configuredGateways=List()),
      Recording(
        id="518a6eb6a63728758313e9fc",
        status="ended",
        startTime=new DateTime(2013, 5, 7, 15, 32, 22),
        endTime=Some(new DateTime(2013, 5, 8, 9, 50, 33)),
        configuredGateways=List()),
      Recording(
        id="518a6eb6a63728758313e9fb",
        status="ended",
        startTime=new DateTime(2013, 5, 6, 14, 23, 22),
        endTime=Some(new DateTime(2013, 5, 6, 15, 50, 33)),
        configuredGateways=List()),
      Recording(
        id="518a6eb6a63728758313e9fa",
        status="ended",
        startTime=new DateTime(2013, 5, 5, 13, 15, 22),
        endTime=Some(new DateTime(2013, 5, 6, 8, 50, 33)),
        configuredGateways=List()),
      Recording(
        id="518a6eb6a63728758313e9f0",
        status="ended",
        startTime=new DateTime(2013, 5, 4, 12, 6, 22),
        endTime=Some(new DateTime(2013, 5, 4, 15, 50, 33)),
        configuredGateways=List())
    )
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
