package service

import scala.concurrent.{Future, ExecutionContext, future}

import com.typesafe.config._

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS

import domain.{SystemStatus, Device, Gateway, RecordedRunConfiguration}

trait SystemService {
  def status(implicit ec: ExecutionContext): Future[SystemStatus]
  def start(implicit ec: ExecutionContext): Future[Unit]
  def stop(implicit ec: ExecutionContext): Future[Unit]
  def attachedDevices(implicit ec: ExecutionContext): Future[List[Gateway]]
  def startRecordedRun(config: RecordedRunConfiguration)
                      (implicit ec: ExecutionContext): Future[Unit]
}

object SystemService extends SystemService {

  val config = ConfigFactory.load()
  val backendUrl = config.getString("backend.url")

  private val statusUrl  = backendUrl + "/system-control/status"
  private val startUrl   = backendUrl + "/system-control/start"
  private val stopUrl    = backendUrl + "/system-control/stop"
  private val devicesUrl = backendUrl + "/system-control/attached-devices"

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
