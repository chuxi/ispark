package cn.edu.zju.ispark.interpreters.spark


import java.io.{ByteArrayOutputStream, File, PrintWriter}
import java.net.URLClassLoader
import java.util
import java.util.Properties

import cn.edu.zju.ispark.interpreters._
import org.apache.commons.lang3.reflect.{FieldUtils, MethodUtils}
import org.apache.spark.repl.{SparkCommandLine, SparkILoop, SparkIMain, SparkJLineCompletion}
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable.ListBuffer
import scala.reflect.api.{Universe => ApiUniverse}
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.Results

/**
 * Created by king on 15-5-28.
 * SparkInterpreter is the interpreter for Spark Code Paragraph
 *
 * @param props define the SparkConf information and other configuration details
 *
 */
class SparkInterpreter(props: Properties) extends Interpreter {

  var interloop: SparkILoop = _

  var intermain: SparkIMain = _

  var completor: SparkJLineCompletion = _

  var sc: SparkContext = _

  var sqlc: SQLContext = _

  val out = new ByteArrayOutputStream()



  setDefaultProperties()


  /**
   * create a sparkContext for the flowing evaluation, the problem is how to determine the classpath
   * @return SparkContext created
   */
  def createSparkContext: SparkContext = {
    logInfo("------ Create new SparkContext " + props.getProperty("spark.master") + " -------")

    val execUri: String = System.getenv("SPARK_EXECUTOR_URI")
    val jars: Array[String] = SparkILoop.getAddedJars

    var classServerUri: String = null

    if (classServerUri == null) {
      try {
        val classServer = intermain.getClass.getMethod("classServerUri")
        classServerUri = classServer.invoke(intermain).toString
      } catch {
        case e: Exception => throw new Exception(e)
      }
    }

    val conf: SparkConf = new SparkConf()
      .setMaster(props.getProperty("spark.master"))
      .setAppName(props.getProperty("spark.app.name"))
      .set("spark.repl.class.uri", classServerUri)

    if (jars.length > 0) {
      conf.setJars(jars)
    }

    if (execUri != null) {
      conf.set("spark.executor.uri", execUri)
    }
    if (System.getenv("SPARK_HOME") != null) {
      conf.setSparkHome(System.getenv("SPARK_HOME"))
    }
    conf.set("spark.scheduler.mode", "FAIR")

    sc = new SparkContext(conf)

    sc
  }

  def createSQLContext: SQLContext = {
    if (sqlc == null) {
      sqlc = new SQLContext(sc)
    }
    sqlc
  }


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

    /**
     * set classpath for settings
     */
    settings.classpath.value = currentClassPath.mkString(File.pathSeparator)


    /**
     * too many restrictions in class SparkILoop,
     * so we have to use reflection to get or set field and invoke private functions
     */

    interloop = new SparkILoop(null, new PrintWriter(out))

    /**
     * implement the body of process(settings: Settings)
     */
    FieldUtils.writeField(interloop, "settings", settings, true)
    MethodUtils.invokeMethod(interloop, "createInterpreter")

    // implicate transformation
    intermain = interloop
    MethodUtils.invokeMethod(intermain, "setContextClassLoader")
    intermain.initializeSynchronous()

    completor = new SparkJLineCompletion(intermain)

    initializeSpark()


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

  def initializeSpark(): Unit = {
    sc = createSparkContext
    sqlc = createSQLContext

    intermain.interpret("@transient var _binder = new java.util.HashMap[String, Object]()")
    val binder = getValue("_binder").asInstanceOf[util.HashMap[String, AnyRef]]
    binder.put("sc", sc)
    binder.put("sqlc", sqlc)

    intermain.beQuietDuring {
      intermain.interpret("""
         @transient val sc = {
           val _sc = _binder.get("sc").asInstanceOf[org.apache.spark.SparkContext]
           println("Spark context available as sc.")
           _sc
         }
                          """)
      intermain.interpret("""
         @transient val sqlc = {
           val _sqlContext = _binder.get("sqlc").asInstanceOf[org.apache.spark.sql.SQLContext]
           println("SQL context available as sqlContext.")
           _sqlContext
         }
                          """)
      intermain.interpret("import org.apache.spark.SparkContext._")
      intermain.interpret("import sqlContext.implicits._")
      intermain.interpret("import sqlContext.sql")
      intermain.interpret("import org.apache.spark.sql.functions._")
    }
  }

  def getValue(name: String): AnyRef = {
    val ret: AnyRef = intermain.valueOfTerm(name)
    ret match {
      case None => null
      case Some(x: AnyRef)  =>  x
      case _  =>  ret
    }
  }






  /**
   * Closes interpreter. You may want to free your resources up here.
   * close() is called only once
   */
  override def close(): Unit = {
//    MethodUtils.invokeMethod(interloop, "closeInterpreter")
    if (intermain ne null) {
      sc.stop()
      intermain.close()
      intermain = null
    }
  }

  /**
   * Run code and return result, in synchronous way.
   *
   * @param st statements to run
   * @return
   */
  override def interpret(st: String): InterpreterResult = {
    out.flush()
    out.reset()

    interpret(st.split("\n").filter(_.trim.nonEmpty))

//    println(out.toString)
  }

  def interpret(st: Array[String]): InterpreterResult = {

    var succ: Boolean = true
    for(s <- st) {
      val r = scala.Console.withOut(out) {
        interloop.interpret(s)
      }
      if (r == Results.Error || r == Results.Incomplete)
        succ = false
    }

    if (succ)
      InterSuccess(out.toString)
    else
      InterError(out.toString)
  }






}
