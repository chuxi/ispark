package cn.edu.zju.ispark.server

import akka.actor._
import cn.edu.zju.ispark.server.calculator.{SessionRequest, InterruptRequest, InterruptCalculator, SparkCalculator}

import scala.concurrent._
import scala.concurrent.duration._

/**
 * Created by king on 15-6-3.
 */
class CalcWebSocketService(system: ActorSystem, initScripts: List[String], compilerArgs: List[String], remoteDeployFuture: Future[Deploy]) {
  implicit val executor = system.dispatcher

  val ioPubPromise = Promise[WebSockWrapper]()
  val shellPromise = Promise[WebSockWrapper]()

  class CalcActor extends Actor with ActorLogging {
    private var currentSessionOperation: Option[ActorRef] = None
    var calculator: ActorRef = null
    var iopub: WebSockWrapper = null
    var shell: WebSockWrapper = null

    private def spawnCalculator(): Unit = {
      val remoteDeploy = Await.result(remoteDeployFuture, 2 minutes)
      calculator = context.actorOf(Props(new SparkCalculator()).withDeploy(remoteDeploy))
    }

    override def preStart(): Unit = {
      iopub = Await.result(ioPubPromise.future, 2 minutes)
      shell = Await.result(shellPromise.future, 2 minutes)
      spawnCalculator()
    }

    override def receive: Receive = {
      case InterruptCalculator =>
        for (op <- currentSessionOperation) {
          calculator.tell(InterruptRequest, op)
        }
      case req@SessionRequest(header, session, request) =>
        val operations = new SessionOperationActors(header, session)

    }


    class SessionOperationActors(header: String, session: String) {
      def singleExecution(counter: Int) = Props(new Actor {
        override def receive: Actor.Receive = {
          ???
        }
      })
    }



  }



}
