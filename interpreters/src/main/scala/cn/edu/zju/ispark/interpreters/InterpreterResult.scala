package cn.edu.zju.ispark.interpreters

/**
 * Created by king on 15-5-28.
 */
sealed trait InterpreterResult extends Serializable

case class InterSuccess(msg: String) extends InterpreterResult

case class InterError(msg: String) extends InterpreterResult

case class InterIncomplete(msg: String) extends InterpreterResult