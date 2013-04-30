package controllers

import java.nio.ByteBuffer

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.iteratee._

// Reactive Mongo imports
import reactivemongo.api._
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONReaderHandler

// Reactive Mongo plugin
import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._

// Play Json imports
import play.api.libs.json._

import domain.SystemStatus

import service.SystemService

object Application extends Controller with MongoController {
 
  private val systemService = SystemService

  lazy val db = ReactiveMongoPlugin.db
  lazy val collection = db("realtime")
  //lazy val cursor = collection.find(Json.obj("address" -> Json.obj("$gt" -> 50562)), QueryOpts().tailable.awaitData)
  //lazy val cursor = collection.find(Json.obj(), QueryOpts().tailable.awaitData)

  def index = Action {
    Ok(views.html.index(systemService.status))
  }

  def realtime = Action { implicit r: RequestHeader =>
    Ok(views.html.realtime(systemService.status))
  }

  def watchRealtimeStream = WebSocket.using[Array[Byte]] { request =>
    val in = Iteratee.ignore[Array[Byte]]

    // Enumerates the capped collection
    val out = {
      val cursor = collection.find(Json.obj("address" -> Json.obj("$gt" -> 50452)), QueryOpts().tailable.awaitData)
      cursor.enumerate.map { jsValue =>
        val value =   (jsValue \ "value").asInstanceOf[JsNumber].value.doubleValue
        val address = (jsValue \ "address").asInstanceOf[JsNumber].value.shortValue
        val bb = ByteBuffer.allocate(2+8)
        bb.putShort(address)
        bb.putDouble(value)
        bb.array
      }
    }

    // We're done!
    (in, out)
  }
}
