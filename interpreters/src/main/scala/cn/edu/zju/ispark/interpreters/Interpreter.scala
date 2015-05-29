package cn.edu.zju.ispark.interpreters

import java.net.URL

import cn.edu.zju.ispark.common.Logging

/**
 * Created by king on 15-5-28.
 */

/**
 * trait for interpreters
 *
 * you must implement open() close() and interpret() three most important interfaces.
 */

trait Interpreter extends Logging {

  var classloaderUrls: Array[URL] = null

  /**
   * Opens interpreter. You may want to place your initialize routine here.
   * open() is called only once
   */
  def open()

  /**
   * Closes interpreter. You may want to free your resources up here.
   * close() is called only once
   */
  def close()

  /**
   * Run code and return result, in synchronous way.
   *
   * @param st statements to run
   */
  def interpret(st: String): Unit




}
