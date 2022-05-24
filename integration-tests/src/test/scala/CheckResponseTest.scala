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
      val response = Await.result(Http(system).singleRequest(HttpRequest(HttpMethods.GET, testUrl), https), 5 seconds )
      assert(response.status.intValue == 200)
    }
  }

}

trait CheckResponseTestScope {
  val testUrl = "https://localhost:8443"
  implicit val system = ActorSystem()
  implicit val dispatcher = system.dispatcher
  val https = {
      val password: Array[Char] = "changeme".toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("client.p12")

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val ts: KeyStore = KeyStore.getInstance("JKS")
    ts.load(null, password)
    ts.setCertificateEntry("cert", loadX509Certificate("demoCA/cacert.pem"))
    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ts)

    val sslContext: SSLContext = SSLContext.getInstance("TLSv1.3")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
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

}
