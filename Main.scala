import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.http._
import akka.http.scaladsl._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._

import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

val https = {
  import java.io.InputStream
  import java.security.{ KeyStore, SecureRandom }

  import javax.net.ssl.{ KeyManagerFactory, SSLContext, TrustManagerFactory }
  import akka.actor.ActorSystem
  import akka.http.scaladsl.server.{ Directives, Route }
  import akka.http.scaladsl.{ ConnectionContext, Http, HttpsConnectionContext }
  implicit val system = ActorSystem()
  implicit val dispatcher = system.dispatcher

  // Manual HTTPS configuration

  val password: Array[Char] = "changeme".toCharArray // do not store passwords in code, read them from somewhere safe!

  val ks: KeyStore = KeyStore.getInstance("PKCS12")
  val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("example.com.p12")

  require(keystore != null, "Keystore required!")
  ks.load(keystore, password)

  val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, password)

  val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  tmf.init(ks)

  val sslContext: SSLContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
  ConnectionContext.httpsServer(sslContext)
}

@main
def main =
  // https://github.com/akka/akka/issues/31395
  val log = LoggerFactory.getLogger("main")
  log.debug("Starting up")

  given sys: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "test")
  Http()
    .newServerAt("localhost", 8443)
    .enableHttps(https)
    .bind(
      headerValueByType[`Tls-Session-Info`](`Tls-Session-Info`)(
        info =>
          if (info.session.getCipherSuite != "TLS_AES_256_GCM_SHA384")
            log.info(info.session.getCipherSuite)
          complete(info.session.getCipherSuite)
    ))
  Await.result(sys.whenTerminated, Duration.Inf)
