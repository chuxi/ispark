package cn.edu.zju.ispark.server

import java.io.File

import cn.edu.zju.ispark.common.{Utils, ISparkConf, Logging}
import org.apache.commons.io.FileUtils
import org.apache.log4j.PropertyConfigurator

/**
 * Created by king on 15-5-21.
 */
object Server extends Logging{
  FileUtils.forceMkdir(new File("logs"))

  def main(args: Array[String]) {
    sys.props.put("ISPARK_HOME", "/home/king/gits/ispark")

    startServer(args, new ISparkConf())
  }

  def startServer(args: Array[String], config: ISparkConf): Unit = {
    PropertyConfigurator.configure(getClass.getResource("/log4j.server.properties"))
    logDebug("Classpath: " + sys.props.get("java.class.path"))

    // TODO: move host port into config
    val defaults = Map("host" -> "127.0.0.1", "port" -> 8081)

    Utils.getPropertiesFromFile().foreach(m => config.setIfMissing(m._1, m._2))


    val app: Dispatcher = new Dispatcher(config)
    val wsPlan = unfiltered.netty.websockets.Planify(app.WebSockets.intent).onPass(_.writeAndFlush(_))
    unfiltered.netty.Server.local(8081).handler(wsPlan).run()



  }



}
