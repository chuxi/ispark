package cn.edu.zju.ispark.server.calculator

import java.util.Properties

import akka.actor.{Props, Actor, ActorLogging}
import cn.edu.zju.ispark.interpreters.{InterError, InterSuccess}
import cn.edu.zju.ispark.interpreters.spark.SparkInterpreter

/**
 * Created by king on 15-6-3.
 */
class SparkCalculator extends Actor with ActorLogging {
  private var eval: SparkInterpreter = _

  // Make a child actor so we don't block the execution on the main thread, so that interruption can work
  private val executor = context.actorOf(Props(new Actor {
    def receive = {
      case ExecuteRequest(_, code) =>
        val result = eval.interpret(code)

        result match {
          case InterSuccess(result)     => sender ! ExecuteResponse(result.toString)
          case InterError(stackTrace) => sender ! ErrorResponse(stackTrace, false)
//          case Incomplete          => sender ! ErrorResponse("", true)
        }
    }
  }))

  override def preStart(): Unit = {
    eval = new SparkInterpreter(new Properties())
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
