import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.http._
import akka.http.scaladsl._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import org.slf4j.LoggerFactory

@main
def main =
  // https://github.com/akka/akka/issues/31395
  val log = LoggerFactory.getLogger("main")
  log.debug("Starting up")

  given sys: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "test")
  Http()
    .newServerAt("localhost", 8443)
    .bind(
      headerValueByType[`Tls-Session-Info`](`Tls-Session-Info`) { info =>
        log.info(info.toString)
        complete("Thanks")
      }
    )
