package cn.edu.zju.ispark.server.calculator

import java.util.Properties

import akka.actor.{Props, Actor, ActorLogging}
import cn.edu.zju.ispark.interpreters.{InterIncomplete, InterError, InterSuccess}
import cn.edu.zju.ispark.interpreters.spark.SparkInterpreter

/**
 * Created by king on 15-6-3.
 */
class SparkCalculator extends Actor with ActorLogging {
  private val eval: SparkInterpreter = new SparkInterpreter(new Properties())

  // Make a child actor so we don't block the execution on the main thread, so that interruption can work
  private val executor = context.actorOf(Props(new Actor {
    def receive = {
      case ExecuteRequest(_, code) =>
        eval.interpret(code) match {
          case InterSuccess(rs)         => sender ! ExecuteResponse(rs)
          case InterError(stackTrace)       => sender ! ErrorResponse(stackTrace, incomplete = false)
          case InterIncomplete(incomplete)  => sender ! ErrorResponse(incomplete, incomplete = true)
        }
    }
  }))

  // if I got some scripts to evaluate before the interpreter, I could add it here.
  override def preStart(): Unit = {
    log.info("pre start of spark calculator")
    eval.open()
  }

  override def postStop(): Unit ={
    log.info("post stop of spark calculator")
    eval.close()
  }


  override def receive: Receive = {
    case InterruptRequest =>

    case req@ExecuteRequest(_, code) => executor.forward(req)
  }
}
