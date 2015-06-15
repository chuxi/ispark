package cn.edu.zju.ispark.server

import java.util.UUID

import cn.edu.zju.ispark.common.Logging
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import unfiltered.netty.websockets.WebSocket

/**
 * Created by king on 15-6-3.
 */


trait WebSockWrapper {
  def send(header: JValue, session: JValue, msgType: String, content: JValue)
}

class WebSockWrapperImpl(sock: WebSocket) extends WebSockWrapper with Logging {

  private def send(m: String) = {
    logTrace("Sending " + m)
    sock.send(m)
  }

  def send(header: JValue, session: JValue, msgType: String, content: JValue): Unit = {
    val respJson = ("parent_header" -> header) ~ ("msg_type" -> msgType) ~ ("msg_id" -> UUID.randomUUID().toString) ~
      ("header" -> ("username" -> "kernel") ~ ("session" -> session))

    send(pretty(render(respJson)))
  }
}
