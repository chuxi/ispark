package cn.edu.zju.ispark.common

import java.io.File
import java.util
import java.util.concurrent.ConcurrentHashMap


/**
 * Created by king on 15-5-22.
 *
 * Load and manage all configurations from xml file
 *
 */

class ISparkNotebookConfig extends Logging {
  private val settings = new ConcurrentHashMap[String, String]()

  require(sys.props.get("ISPARK_HOME").nonEmpty, "Please set the ISPARK_HOME property~")

  // auto load the default configuration file
  Utils.getPropertiesFromFile().foreach(m => set(m._1, m._2))

  def set(property: String, value: String): Unit = {
    settings.put(property, value)
  }

  /** Set a parameter if it isn't already configured */
  def setIfMissing(key: String, value: String): ISparkNotebookConfig = {
    settings.putIfAbsent(key, value)
    this
  }

  def notebooksDir = settings.get("notebooks.dir") match {
    case null => new File(".")
    case s: String => new File(s)
  }

  def projectName = settings.get("notebooks.name") match {
    case null => notebooksDir.getPath
    case s: String => s
  }

  def printsettings(): Unit = println(util.Arrays.toString(settings.entrySet().toArray))

  def get(property: String) = settings.get(property)

  def getInt(property: String) = get(property).toInt


}
