
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.http.scaladsl.{ConnectionContext, Http, HttpExt, HttpsConnectionContext}
import org.scalatest._

import java.io.InputStream
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
  val testUrl = "https://here.mtls.proxy.com:8443"
  implicit val system = ActorSystem()
  implicit val dispatcher = system.dispatcher
  val https = {
      val password: Array[Char] = "testpass".toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("clientcert.p12")

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val ts: KeyStore = KeyStore.getInstance("JKS")
    val truststore: InputStream = getClass.getClassLoader.getResourceAsStream("keystore.jks")
    ts.load(truststore, password)
    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ts)

    val sslContext: SSLContext = SSLContext.getInstance("TLSv1.3")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    //ConnectionContext.httpsClient(sslContext)
    // I know is deprecated but I couldn't find a quick way to set the enabled protocol and cipher suites
    ConnectionContext.https(sslContext, enabledProtocols = Some(List("TLSv1.3")), enabledCipherSuites = Some(List("TLS_AES_256_GCM_SHA384")))

  }

  def akkaHttpClient(context: HttpsConnectionContext): HttpExt = {
    val akkaHttp = Http()
    akkaHttp.setDefaultClientHttpsContext(context)
    akkaHttp
  }

  lazy val client = akkaHttpClient(https)
}