package cn.edu.zju.ispark.server


import java.util.concurrent.atomic.AtomicInteger

import cn.edu.zju.ispark.common.{Logging, ISparkConf}
import unfiltered.netty.websockets._
import unfiltered.request.{GET, Seg, Path}


/**
 * Created by king on 15-5-23.
 */
class Dispatcher(config: ISparkConf) extends Logging{

  val executionCounter = new AtomicInteger(0)

  import scala.collection.JavaConversions._
  val sockets: scala.collection.concurrent.Map[Int, WebSocket] =
    new java.util.concurrent.ConcurrentHashMap[Int, WebSocket]

  def notify(msg: String) = sockets.values.foreach { s =>
    if(s.channel.isActive) s.send(msg)
  }

  object WebSockets {
    val intent: unfiltered.netty.websockets.Intent = {
      case _ => {
        case Open(s) =>
          logInfo(s"open a socket $executionCounter)")
          s.send("sys|hola!")
        case Message(s, Text(msg)) =>
          logInfo(s"received message $msg from socket $s")
        case Close(s) =>
          logInfo(s"close socket $s")
        case Error(s, e) =>
          e.printStackTrace
      }

    }
  }

}
