package cn.edu.zju.ispark.server

import java.io.File

import cn.edu.zju.ispark.common.{ISparkNotebookConfig, Logging}
import org.apache.commons.io.FileUtils
import org.apache.log4j.PropertyConfigurator

/**
 * Created by king on 15-5-21.
 */
object Server extends Logging{
  FileUtils.forceMkdir(new File("logs"))

  def main(args: Array[String]) {
    // move this to binary file instead
    sys.props.put("ISPARK_HOME", "/home/king/gits/ispark")

    startServer(args, new ISparkNotebookConfig())((http, app) => Unit)
  }

  def openBrowser(url: String) {
    println("Launching browser on %s".format(url))
    unfiltered.util.Browser.open(url) match {
      case Some(ex) => println("Cannot open browser to %s\n%s".format(url, ex.toString))
      case None =>
    }
  }

  /**
   *
   * @param args pass into args
   * @param config ISparkNotebookConfig, auto load the file
   * @param afterStart justify the server started correctly by return Boolean (test)
   * @return
   */
  def startServer(args: Array[String], config: ISparkNotebookConfig)(afterStart: (unfiltered.netty.Server, Dispatcher) => Unit): Unit = {
    PropertyConfigurator.configure(getClass.getResource("/log4j.server.properties"))
    logDebug("Classpath: " + sys.props.get("java.class.path"))

    if (!config.notebooksDir.exists()) {
      logWarning("Base directory %s for Scala Notebook server does not exist.  Creating, but your server may be misconfigured.".format(config.notebooksDir))
      config.notebooksDir.mkdirs()
    }

    val app: Dispatcher = new Dispatcher(config)
    val wsPlan = unfiltered.netty.websockets.Planify(app.WebSockets.intent).onPass(_.fireChannelRead(_))

    val nbReadPlan = unfiltered.netty.cycle.Planify(app.WebServer.nbReadIntent)
    val nbWritePlan = unfiltered.netty.cycle.Planify(app.WebServer.nbWriteIntent)
    val otherPlan = unfiltered.netty.cycle.Planify(app.WebServer.otherIntent)


    /**
     * To set different ports for http and websocket, we run another websocket server aside http server
     */
    val websocket = unfiltered.netty.Server.http(config.getInt("port") + 1, config.get("host"))
      .handler(wsPlan)
      .chunked(256 << 20)
      .start()


    unfiltered.netty.Server.http(config.getInt("port"), config.get("host"))
      .handler(nbReadPlan)
      .handler(nbWritePlan)

      .handler(otherPlan)
      .resources(getClass.getResource("/www/"), 3600, true)

      .run({svr => {
        afterStart(svr, app)
//        openBrowser("http://%s:%d".format("127.0.0.1", 8080))
        println("http port: " + svr.ports.head)
        println("websocket port: " + websocket.ports.head)
      }},
      {svr => {
        websocket.stop()
      }})

  }


}
