package cn.edu.zju.ispark.interpreters.spark

import java.io._
import java.net.{URL, URLClassLoader}
import java.util.Properties

import cn.edu.zju.ispark.interpreters.{InterpreterResult, InterpreterContext, Interpreter}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.repl.{SparkCommandLine, SparkIMain, SparkILoop}
import org.apache.spark.sql.SQLContext

import scala.collection.mutable.{ListBuffer, ArrayBuffer}
import scala.tools.nsc.Settings

/**
 * Created by king on 15-5-28.
 * SparkInterpreter is the interpreter for Spark Code Paragraph
 *
 * @param props define the SparkConf information and other configuration details
 * @param sc define the SparkContext which could be created or passed into
 *
 */
class SparkInterpreter(props: Properties, var sc: Option[SparkContext] = None) extends Interpreter {

  var interloop: SparkILoop = _

  var intermain: SparkIMain = _

  var sqlc: Option[SQLContext] = None

  val out = new StringWriter()


  setDefaultProperties()


  def getSparkContext = sc.get

  def isSparkContextInitialized = sc.isDefined

  def getSQLContext = sqlc.get

  /**
   * create a sparkContext for the flowing evaluation, the problem is how to determine the classpath
   * @return SparkContext created
   */
//  def createSparkContext: SparkContext = {
//    logInfo("------ Create new SparkContext " + props.getProperty("spark.master") + " -------")
//
//    val execUri: String = System.getenv("SPARK_EXECUTOR_URI")
//    val jars: Array[String] = SparkILoop.getAddedJars
//
//    var classServerUri: String = null
//
//    if (classServerUri == null) {
//      try {
//        val classServer = intermain.getClass.getMethod("classServerUri")
//        classServerUri = classServer.invoke(intermain).toString
//      } catch {
//        case e: Exception => throw new Exception(e)
//      }
//    }
//
//    val conf: SparkConf = new SparkConf()
//      .setMaster(props.getProperty("spark.master"))
//      .setAppName(props.getProperty("spark.app.name"))
//      .set("spark.repl.class.uri", classServerUri)
//
//    if (jars.length > 0) {
//      conf.setJars(jars)
//    }
//
//    if (execUri != null) {
//      conf.set("spark.executor.uri", execUri)
//    }
//    if (System.getenv("SPARK_HOME") != null) {
//      conf.setSparkHome(System.getenv("SPARK_HOME"))
//    }
//    conf.set("spark.scheduler.mode", "FAIR")
//
//    sc = Some(new SparkContext(conf))
//
//    sc.get
//  }

  /**
   * set default properties for the spark context
   */
  def setDefaultProperties() = {
    // spark.master and spark.app.name are two required properties
    // if they are not set, apply default setting
    if (props.getProperty("spark.master") == null)
      props.setProperty("spark.master", "local[*]")
    if (props.getProperty("spark.app.name") == null)
      props.setProperty("spark.app.name", "ispark-notebook")

    // spark executor memory per worker instance. ex) 512m, 32g
    if (props.getProperty("spark.executor.memory") == null)
      props.setProperty("spark.executor.memory", "512m")
    // Total number of cores to use. Empty value uses all available core
    if (props.getProperty("spark.cores.max") == null)
      props.setProperty("spark.cores.max", "2")
    if (props.getProperty("args") == null)
      props.setProperty("args", "")
  }





  /**
   * Opens interpreter. You may want to place your initialize routine here.
   * open() is called only once
   * initial the environment
   */
  override def open(): Unit = {

    /**
     * SparkCommandLine is used for get the classpath from args?
     * To be tested
     */
    var settings: Settings = new Settings()
    if (props.getProperty("args") != null) {
      val argsArray: Array[String] = props.getProperty("args").split(" ")
      val argList: ListBuffer[String] = ListBuffer[String]()
      for (arg <- argsArray) {
        argList += arg
      }
      val command: SparkCommandLine = new SparkCommandLine(argList.toList)
      settings = command.settings
    }
    val pathSettings = settings.classpath

    /**-------------------------------------**/

    var classpath = ""
    val paths = currentClassPath.mkString(File.pathSeparator)

    settings.classpath.value = paths

    // use an None input to initial the process, so we could use the inner function to createInterpreter
    interloop = new SparkILoop(None , new PrintWriter(out), Some(props.getProperty("spark.master")))
    org.apache.spark.repl.Main.interp = interloop
    interloop.process(Array("-classpath", paths))

    // implicate transformation
    intermain = interloop

//    interloop.interpret()
//    intermain.interpret()
    sc = Some(interloop.sparkContext)
    sqlc = Some(interloop.sqlContext)
  }

  def currentClassPath: List[File] = {
    val paths = classPath(Thread.currentThread().getContextClassLoader)
    val cps = System.getProperty("java.class.path").split(File.pathSeparator)
    if (cps != null)
      for (cp <- cps)
        paths += new File(cp)
    paths.toList
  }

  def classPath(cl: ClassLoader): ListBuffer[File] = {
    val paths = ListBuffer[File]()

    if (cl == null)
      return paths

    if (cl.isInstanceOf[URLClassLoader]) {
      val ucl = cl.asInstanceOf[URLClassLoader]
      val urls = ucl.getURLs
      if (urls != null)
        for (url <- urls)
          paths += new File(url.getFile)
    }
    paths
  }

  /**
   * Closes interpreter. You may want to free your resources up here.
   * close() is called only once
   */
  override def close(): Unit = {
    org.apache.spark.repl.Main.interp = null
    if (interloop.sparkContext != null)
      interloop.sparkContext.stop()
  }

  /**
   * Run code and return result, in synchronous way.
   *
   * @param st statements to run
   * @return
   */
  override def interpret(st: String): Unit = {
    // write into console

    println(out.toString)
  }
}
