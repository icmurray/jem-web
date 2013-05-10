package controllers

import java.nio.ByteBuffer

import org.joda.time.{DateTime, DateTimeUtils}

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.iteratee._

// Reactive Mongo imports
import reactivemongo.api._
import reactivemongo.bson._
import play.modules.reactivemongo._
import play.modules.reactivemongo.json.collection.JSONCollection

// Play Json imports
import play.api.libs.json._

import domain.SystemStatus

import service.SystemService

object Application extends Controller
                   with MongoController
                   with ControllerUtilities {
 
  private val systemService = SystemService

  override lazy val db = ReactiveMongoPlugin.db
  lazy val collection = db.collection[JSONCollection]("realtime")

  def index = Action { implicit request =>
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
  
      systemService.currentRecording.map { recordingO =>
        Ok(views.html.realtime(recordingO))
      } recover {
        case t => backendIsDownResponse
      }
    }
  }

  def watchRealtimeStream = WebSocket.using[JsValue] { request =>
    // Enumerates the capped collection
    val now = new DateTime()

    val query = Json.obj(
      "timing_info.end" -> Json.obj(
        "$gt" -> (DateTimeUtils.getInstantMillis(now) / 1000L)
      )
    )

    val projection = Json.obj(
      "values" -> 1,
      "timing_info.end" -> 1,
      "_id" -> 0,
      "device" -> 1,
      "table_id" -> 1
    )

    val cursor: Cursor[JsValue] = collection.find(query, projection).options(QueryOpts().tailable.awaitData).cursor[JsValue]
    val out = cursor.enumerate
    

    val in = Iteratee.ignore[JsValue].mapDone { _ =>
      println("DISCONNECTED!")
      cursor.close()
    }

    // We're done!
    (in, out)
  }
}
