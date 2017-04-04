import play.api.mvc.{Action, Results}
import play.api.routing.Router
import play.api.routing.sird._
import play.api.{BuiltInComponents, Mode}
import play.core.server.{NettyServerComponents, ServerConfig}

object HelloPlay extends App {

  val components = new NettyServerComponents with BuiltInComponents {

    def port() = sys.env.getOrElse("PORT", "9000").toInt

    def mode() = if (configuration.getString("play.crypto.secret").contains("changeme")) Mode.Dev else Mode.Prod

    override lazy val serverConfig = ServerConfig(port = Some(port()), mode = mode())

    lazy val router = Router.from {
      case GET(p"/") => Action {
        Results.Ok("hello, world")
      }
    }
  }

  val server = components.server

  while (!Thread.currentThread.isInterrupted) {}

  server.stop()
}
