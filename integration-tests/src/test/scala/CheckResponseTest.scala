import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.http.scaladsl.{ConnectionContext, Http, HttpExt, HttpsConnectionContext}
import org.scalatest._

import java.io.InputStream
import java.security.cert.{Certificate, CertificateFactory}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class CheckResponseTest extends wordspec.AnyWordSpec with CheckResponseTestScope {

  "Server" should {
    "return success response" in {
      val response = Await.result(client.singleRequest(HttpRequest(HttpMethods.GET, testUrl)), 5 seconds )
      assert(response.status.intValue == 200)
    }
  }

}

trait CheckResponseTestScope {
  val testUrl = "https://example.com:8443"
  implicit val system = ActorSystem()
  implicit val dispatcher = system.dispatcher
  val sslContext = {
    import java.io.InputStream
    import java.security.{ KeyStore, SecureRandom }
  
    import javax.net.ssl.{ KeyManagerFactory, SSLContext, TrustManagerFactory }
    import akka.actor.ActorSystem
    import akka.http.scaladsl.server.{ Directives, Route }
    import akka.http.scaladsl.{ ConnectionContext, Http, HttpsConnectionContext }
    implicit val system = ActorSystem()
    implicit val dispatcher = system.dispatcher
  
    // Manual HTTPS configuration

    val ks: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    ks.load(null, "abcdef".toCharArray)
    ks.setCertificateEntry("cert", loadX509Certificate("client.crt.pem"))
    ks.setCertificateEntry("ca", loadX509Certificate("demoCA/cacert.pem"))

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, "abcdef".toCharArray)

    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    keyStore.load(null, null)
    keyStore.setCertificateEntry("cert", loadX509Certificate("demoCA/cacert.pem"))
    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(keyStore)

    val result: SSLContext = SSLContext.getInstance("TLSv1.3")
    result.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    result
  }
  val https = {
    //ConnectionContext.httpsClient(sslContext)
    // I know is deprecated but I couldn't find a quick way to set the enabled protocol and cipher suites
    ConnectionContext.https(sslContext, enabledProtocols = Some(List("TLSv1.3")), enabledCipherSuites = Some(List("TLS_AES_256_GCM_SHA384")))
  }

  def resourceStream(resourceName: String): InputStream = {
    val is = getClass.getClassLoader.getResourceAsStream(resourceName)
    require(is ne null, s"Resource $resourceName not found")
    is
  }
  def loadX509Certificate(resourceName: String): Certificate =
    CertificateFactory.getInstance("X.509").generateCertificate(resourceStream(resourceName))

  def akkaHttpClient(context: HttpsConnectionContext): HttpExt = {
    val akkaHttp = Http()
    akkaHttp.setDefaultClientHttpsContext(context)
    akkaHttp
  }

  lazy val client = akkaHttpClient(https)
}
