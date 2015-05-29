package cn.edu.zju.ispark.common

import java.util.concurrent.ConcurrentHashMap


/**
 * Created by king on 15-5-22.
 *
 * Load and manage all configurations from xml file
 *
 */

class ISparkConf extends Logging {
  private val settings = new ConcurrentHashMap[String, String]()


  def set(property: String, value: String): Unit = {
    settings.put(property, value)
    settings
  }

  /** Set a parameter if it isn't already configured */
  def setIfMissing(key: String, value: String): ISparkConf = {
    settings.putIfAbsent(key, value)
    this
  }


}
