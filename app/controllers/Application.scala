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

object Application extends Controller
                   with MongoController
                   with ControllerUtilities {
 
  private val systemService = SystemService
  override lazy val db = ReactiveMongoPlugin.db
  lazy val collection = db("realtime")
  //lazy val cursor = collection.find(Json.obj("address" -> Json.obj("$gt" -> 50562)), QueryOpts().tailable.awaitData)
  //lazy val cursor = collection.find(Json.obj(), QueryOpts().tailable.awaitData)

  def index = Action {
    Async {
      systemService.status.map { status =>
        Ok(views.html.index(status))
      } recover {
        case t => backendIsDownResponse
      }
    }
  }

  def realtime = Action { implicit r: RequestHeader =>
    Async {
      systemService.status.map { status =>
        Ok(views.html.realtime(status))
      } recover {
        case t => backendIsDownResponse
      }
    }
  }

  def watchRealtimeStream = WebSocket.using[Array[Byte]] { request =>
    // Enumerates the capped collection
    //val cursor = collection.find(BSONDocument("address" -> BSONDocument("$gt" -> 50452)), QueryOpts().tailable.awaitData)
    //val cursor = collection.find(BSONDocument("address" -> BSONDocument("$gt" -> 50452))).options(QueryOpts().tailable.awaitData).cursor[BSONDocument]
    val cursor = collection.find(BSONDocument()).options(QueryOpts().tailable.awaitData).cursor[BSONDocument]
    val out = cursor.enumerate.map { bson =>

      try {
        val values = bson.getAs[BSONArray]("values").get
        val bb = ByteBuffer.allocate(4 + (2+4) * values.length)
        bb.putInt(values.length)

        var i = 0
        while (i < values.length) {
          val bsonAry = values.getAs[BSONArray](i).get
          val address = bsonAry.getAs[Int](0).get
          val value   = bsonAry.getAs[Int](1).get
          bb.putShort(address.asInstanceOf[Short])
          bb.putInt(value)
          i += 1
        }

        bb.array
      } catch {
        case e: Throwable => println(e) ; throw e
      }
    }

    val in = Iteratee.ignore[Array[Byte]].mapDone { _ =>
      println("DISCONNECTED!")
      cursor.close()
    }

    // We're done!
    (in, out)
  }
}
