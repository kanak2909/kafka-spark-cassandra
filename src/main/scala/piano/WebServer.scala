package piano

import akka.stream.scaladsl.{Flow, Source}
import controllers.Assets
import play.api.libs.json._
import play.api.mvc.{Action, Results, WebSocket}
import play.api.routing.Router
import play.api.routing.sird._
import play.api.{BuiltInComponents, Mode}
import play.core.server.{NettyServerComponents, ServerConfig}

import scala.concurrent.Future


object WebServer extends App {

  val components = new NettyServerComponents with BuiltInComponents {

    def port() = sys.env.getOrElse("PORT", "8080").toInt

    def mode() = if (configuration.getString("play.crypto.secret").contains("changeme")) Mode.Dev else Mode.Prod

    lazy val kafkaHelper = KafkaHelper(actorSystem, materializer)

    override lazy val serverConfig = ServerConfig(port = Some(port()), mode = mode())

    override lazy val router = Router.from {
      case GET(p"/") => Assets.at("/public", "index.html")
      case GET(p"/lib/$path*") => Assets.at("/lib", path)
      case GET(p"/songs") => Action.async {
        val pianoSongs: Iterable[String] = CassandraHelper.getAllPianoSongs()
        Future.successful(Results.Ok(Json.arr(pianoSongs)))
      }
      case GET(p"/play/$id") => Action {
        val song = CassandraHelper.getPianoSong(id)
        Results.Ok(Json.toJson(song))
      }
      case GET(p"/ws") => WebSocket.accept[JsValue, JsValue] { _ =>
        val wsSink = kafkaHelper.sink().contramap[JsValue](KafkaHelper.fromJson(KafkaHelper.recordTopic))
        Flow.fromSinkAndSource(wsSink, Source.maybe[JsValue])
      }
    }
  }

  val server = components.server
  while (!Thread.currentThread.isInterrupted) {}
  server.stop()

}
