package cn.edu.zju.ispark.server.calculator

import org.json4s.JsonAST.JValue

/**
 * Created by king on 15-6-3.
 */


sealed trait CalcServiceMessage
case class SessionRequest(header: JValue, session: JValue, kernelRequest: CalcRequest) extends CalcServiceMessage
case object InterruptCalculator extends CalcServiceMessage

sealed trait CalcRequest
case class ExecuteRequest(counter: Int, code: String) extends CalcRequest
case class CompletionRequest(line: String, cursorPosition: Int) extends CalcRequest
case class ObjectInfoRequest(objName: String) extends CalcRequest
case object InterruptRequest extends CalcRequest

sealed trait CalcResponse
case class StreamResponse(data: String, name: String) extends CalcResponse
case class ExecuteResponse(html: String) extends CalcResponse
case class ErrorResponse(message: String, incomplete: Boolean) extends CalcResponse

trait Calculator