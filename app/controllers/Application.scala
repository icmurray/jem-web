package controllers

import java.nio.ByteBuffer

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.iteratee._

// Reactive Mongo imports
import reactivemongo.api._
import reactivemongo.bson._
//import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONReaderHandler

// Reactive Mongo plugin
import play.modules.reactivemongo._
//import play.modules.reactivemongo.PlayBsonImplicits._

// Play Json imports
import play.api.libs.json._

import domain.SystemStatus

import service.SystemService

object Application extends Controller with MongoController {
 
  private val systemService = SystemService

  override lazy val db = ReactiveMongoPlugin.db
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
    // Enumerates the capped collection
    //val cursor = collection.find(BSONDocument("address" -> BSONDocument("$gt" -> 50452)), QueryOpts().tailable.awaitData)
    val cursor = collection.find(BSONDocument("address" -> BSONDocument("$gt" -> 50452))).options(QueryOpts().tailable.awaitData).cursor[BSONDocument]
    val out = cursor.enumerate.map { bson =>
      val value = bson.getAs[Int]("value").get.asInstanceOf[Double]
      val address = bson.getAs[Int]("address").get.asInstanceOf[Short]
      //val value =   (bson \ "value").asInstanceOf[JsNumber].value.doubleValue
      //val address = (bson \ "address").asInstanceOf[JsNumber].value.shortValue
      val bb = ByteBuffer.allocate(2+8)
      bb.putShort(address)
      bb.putDouble(value)
      bb.array
    }

    val in = Iteratee.ignore[Array[Byte]].mapDone { _ =>
      println("DISCONNECTED!")
      cursor.close()
    }

    // We're done!
    (in, out)
  }
}
