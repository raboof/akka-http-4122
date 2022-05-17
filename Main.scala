import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.http._
import akka.http.scaladsl._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.security.cert.{CertificateFactory, X509Certificate}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.matching.Regex

object Main extends App {
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
  
    val password: Array[Char] = "testpass".toCharArray // do not store passwords in code, read them from somewhere safe!
  
    val ks: KeyStore = KeyStore.getInstance("JKS")
    val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("server.p12")
  
    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)
  
    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)
  
    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")

    val certificates: Seq[X509Certificate] = {
      val dashes = "[-]{5}"
      val splitCertificatePattern: Regex = s"(?s)${dashes}BEGIN CERTIFICATE$dashes.+?${dashes}END CERTIFICATE$dashes(?-s)".r
      val cf = CertificateFactory.getInstance("X.509")
      val cert = Source
        .fromInputStream(getClass.getClassLoader.getResourceAsStream("all_ca.crt"))
        .toList
        .mkString("")

      val certificates = splitCertificatePattern
        .findAllIn(cert)
        .toList
        .map(_.getBytes)

      certificates.map { b =>
        cf.generateCertificate(new ByteArrayInputStream(b))
          .asInstanceOf[X509Certificate]
      }
    }

    val keyStoreFromPem: KeyStore = {
      val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
      keyStore.load(null, password)
      certificates.zipWithIndex.foreach {
        case (certificate, index) =>
          keyStore.setCertificateEntry(s"cert$index", certificate)
      }
      keyStore
    }
    tmf.init(keyStoreFromPem)
  
    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)

    ConnectionContext.httpsServer(() => {
      val engine = sslContext.createSSLEngine()
      engine.setUseClientMode(false)
      engine.setNeedClientAuth(true)
      engine
    })
  }

  // https://github.com/akka/akka/issues/31395
  val log = LoggerFactory.getLogger("main")
  log.debug("Starting up")

  implicit val sys: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "test")
  Http()
    .newServerAt("localhost", 8443)
    .enableHttps(https)
    .bind(
      headerValueByType[`Tls-Session-Info`](`Tls-Session-Info`)(
        info => {
          if (info.session.getCipherSuite != "TLS_AES_256_GCM_SHA384") {
            log.info(info.session.getCipherSuite)
            complete(InternalServerError)
          } else {
            //log.info(info.session.getPeerCertificates.size.toString)
            complete(info.session.getCipherSuite)
          }
        }
    ))
  Await.result(sys.whenTerminated, Duration.Inf)
}
