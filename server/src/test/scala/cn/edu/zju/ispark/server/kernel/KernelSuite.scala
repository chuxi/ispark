package cn.edu.zju.ispark.server.kernel

import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue

import akka.actor.ActorSystem
import akka.pattern.AskSupport
import akka.testkit.{ImplicitSender, TestKit}
import cn.edu.zju.ispark.server.calculator.{ExecuteRequest, SessionRequest}
import cn.edu.zju.ispark.server.{CalcWebSocketService, WebSockWrapper}
import com.typesafe.config.ConfigFactory
import org.json4s.JsonAST.{JInt, JValue}
import org.scalatest._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Created by king on 15-6-11.
 */
class KernelSuite(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FunSuiteLike with Matchers with BeforeAndAfterAll with AskSupport {

  def this() = this(ActorSystem("KernelTest", ConfigFactory.load("akka-server")))

  var startedKernels = List[Kernel]()

  var kernel: Kernel = _

  val kernelId = UUID.randomUUID().toString

  var service: CalcWebSocketService = _

  var sparkwebsock: SparkWebSocket = _

  override def beforeAll(): Unit = {
    sparkwebsock = new SparkWebSocket("spark")
    val kernel = new Kernel(system)
    KernelManager.add(kernelId, kernel)
    println(kernel.remoteDeployFuture)
    service = new CalcWebSocketService(system, List(), List(), kernel.remoteDeployFuture)
    service.sparkPromise.success(sparkwebsock)
  }

  override def afterAll() {
    println("Shutting down %d kernels".format(startedKernels.size))
    KernelManager.shutdown()
  }

  class SparkWebSocket(name: String) extends WebSockWrapper {
    val q = new LinkedBlockingQueue[JValue]()
    override def send(header: JValue, session: JValue, msgType: String, content: JValue) {
      println("%s: %s".format(name, content))
      q.add(content)
    }
    def response() = Future { q.take() }
    def filteredResponse(filter: JValue => Boolean) = Future {
      var r: JValue = null
      do {
        r = q.take()
      } while (!filter(r))
      r
    }
  }


  def sendCode(code:String) {
    service.calcActor ! SessionRequest(JInt(1), JInt(1), ExecuteRequest(1, code))
  }

  def assertCode(input: String, output: String): Unit = {
    sendCode(input)
    val r = Await.result(sparkwebsock.response(), 10.seconds)
    assert(r.toString.contains(output.trim))
  }

  test("simple code interpreter") {
    assertCode("1+1", "res0: Int = 2")
  }





}
