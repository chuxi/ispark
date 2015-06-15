package cn.edu.zju.ispark.server

import akka.actor._
import cn.edu.zju.ispark.server.calculator._
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._

import scala.concurrent._
import scala.concurrent.duration._

/**
 * Created by king on 15-6-3.
 */
class CalcWebSocketService(system: ActorSystem, initScripts: List[String], compilerArgs: List[String], remoteDeployFuture: Future[Deploy]) {
  implicit val executor = system.dispatcher

//  val ioPubPromise = Promise[WebSockWrapper]()
//  val shellPromise = Promise[WebSockWrapper]()
  val sparkPromise = Promise[WebSockWrapper]()

  val calcActor = system.actorOf(Props(new CalcActor()))

  class CalcActor extends Actor with ActorLogging {
    private var currentSessionOperation: Option[ActorRef] = None
    var calculator: ActorRef = null
//    var iopub: WebSockWrapper = null
//    var shell: WebSockWrapper = null
    var spark: WebSockWrapper = null

    private def spawnCalculator(): Unit = {
      val remoteDeploy = Await.result(remoteDeployFuture, 2 minutes)
      log.info("start calculato")
      calculator = context.actorOf(Props(new SparkCalculator()).withDeploy(remoteDeploy))
      log.info("calculator path: " + calculator.toString())
    }

    override def preStart(): Unit = {
//      iopub = Await.result(ioPubPromise.future, 2 minutes)
//      shell = Await.result(shellPromise.future, 2 minutes)
      spark = Await.result(sparkPromise.future, 2 minutes)
      spawnCalculator()
    }

    override def receive: Receive = {
      case InterruptCalculator =>
        for (op <- currentSessionOperation) {
          calculator.tell(InterruptRequest, op)
        }
      case req@SessionRequest(header, session, request) =>
        val operations = new SessionOperationActors(header, session)
        val operationActor = (request: @unchecked) match {
          case ExecuteRequest(counter, code) =>
            operations.singleExecution(counter)
        }
        val operation = context.actorOf(operationActor)
        context.watch(operation)
        currentSessionOperation = Some(operation)
        calculator.tell(request, operation)


      case Terminated(actor) =>
        log.warning("Termination")
        if (actor == calculator) {
          spawnCalculator()
        } else {
          currentSessionOperation = None
        }

    }


    class SessionOperationActors(header: JValue, session: JValue) {
      def singleExecution(counter: Int) = Props(new Actor {
        override def receive: Actor.Receive = {
          case StreamResponse(data, name) =>
//            iopub.send(header, session, "stream", ("data" -> data) ~ ("name" -> name))

          case ExecuteResponse(html) =>
//            iopub.send(header, session, "pyout", ("execution_count" -> counter) ~ ("data" -> ("text/html" -> html)))
//            iopub.send(header, session, "status", ("execution_state" -> "idle"))
//            shell.send(header, session, "execute_reply", ("execution_count" -> counter))
            spark.send(header, session, "spout", ("execution_count" -> counter) ~ ("data" -> ("text/html" -> html)))
            context.stop(self)

          case ErrorResponse(msg, incomplete) =>
//            if (incomplete) {
//              iopub.send(header, session, "pyincomplete", ("execution_count" -> counter) ~ ("status" -> "error"))
//            } else {
//              iopub.send(header, session, "pyerr", ("execution_count" -> counter) ~ ("status" -> "error") ~ ("ename" -> "Error") ~ ("traceback" -> Seq(msg)))
//            }
//            iopub.send(header, session, "status", ("execution_state" -> "idle"))
//            shell.send(header, session, "execute_reply", ("execution_count" -> counter))
            spark.send(header, session, "sperr", ("execution_count" -> counter) ~ ("traceback" -> Seq(msg)))
            context.stop(self)
        }
      })
    }

  }



}
