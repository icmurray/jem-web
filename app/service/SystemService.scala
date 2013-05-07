package service

import scala.concurrent.{Future, ExecutionContext, future}

import com.typesafe.config._

import play.api.libs.ws.WS

import domain.{SystemStatus, Device, Gateway, RecordedRunConfiguration}

trait SystemService {
  def status(implicit ec: ExecutionContext): Future[SystemStatus]
  def start(implicit ec: ExecutionContext): Future[Unit]
  def stop(implicit ec: ExecutionContext): Future[Unit]
  def attachedDevices(implicit ec: ExecutionContext): Future[List[Device]]
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

  //private implicit val devicesReads = (
  //  (__ \ "unit").read[Int]
  //)(Devi

  override def attachedDevices(implicit ec: ExecutionContext) = future {

    WS.url(devicesUrl).get().map { response =>
      
    }

    List(
      Device(unit=1, gateway=Gateway("127.0.0.1", 502)),
      Device(unit=2, gateway=Gateway("127.0.0.1", 502)),
      Device(unit=1, gateway=Gateway("192.168.0.191", 5020))
    )
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
