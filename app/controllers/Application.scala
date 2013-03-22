package controllers

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

object Application extends Controller with MongoController {
  
  val db = ReactiveMongoPlugin.db
  lazy val collection = db("realtime")
  //lazy val cursor = collection.find(Json.obj("address" -> Json.obj("$gt" -> 50562)), QueryOpts().tailable.awaitData)
  //lazy val cursor = collection.find(Json.obj(), QueryOpts().tailable.awaitData)

  def index = Action {
    Ok(views.html.index())
  }

  def watchRealtimeStream = WebSocket.using[JsValue] { request =>

    val in = Iteratee.foreach[JsValue] { json =>
      println("received " + json)
      collection.insert(json)
    }

    // Enumerates the capped collection
    val out = {
      val cursor = collection.find(Json.obj("address" -> Json.obj("$gt" -> 50552)), QueryOpts().tailable.awaitData)
      cursor.enumerate
    }

    // We're done!
    (in, out)
  }
}
