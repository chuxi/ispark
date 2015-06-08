package cn.edu.zju.ispark.server

import cn.edu.zju.ispark.common.{Logging, Utils}
import com.fasterxml.jackson.annotation.JsonValue
import unfiltered.netty.websockets.WebSocket

/**
 * Created by king on 15-6-3.
 */

trait SockMessgae
case class SessionSockMessage(header: JsonValue, session: String, msgType: String, content: String) extends SockMessgae

trait WebSockWrapper {
  def send(m: SockMessgae)
}

class WebSockWrapperImpl(sock: WebSocket) extends WebSockWrapper with Logging {
  private val mapper = Utils.mapper

  def send(m: SockMessgae) = {
    logTrace("Sending " + m)
    sock.send(mapper.writeValueAsString(m))
  }

  def send(header: String, session: String, msgType: String, content: String): Unit = {
    send(SessionSockMessage(header, session, msgType, content))
  }
}
