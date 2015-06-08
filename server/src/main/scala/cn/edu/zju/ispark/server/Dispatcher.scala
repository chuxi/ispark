package cn.edu.zju.ispark.server


import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import cn.edu.zju.ispark.common.{ISparkNotebookConfig, Logging, Utils}
import com.typesafe.config.ConfigFactory
import unfiltered.netty.websockets._
import unfiltered.request._
import unfiltered.response._


/**
 * Created by king on 15-5-23.
 */
class Dispatcher(config: ISparkNotebookConfig) extends Logging {

  val executionCounter = new AtomicInteger(0)

  val mapper = Utils.mapper
  val nbm = new NotebookManager(config.projectName, config.notebooksDir)
  val system = ActorSystem("ISparkNotebookServer", AkkaConfigUtils.optSecureCookie(ConfigFactory.load("akka-server.conf"), akka.util.Crypt.generateSecureCookie))

  import scala.collection.JavaConversions._
  val sockets: scala.collection.concurrent.Map[Int, WebSocket] =
    new java.util.concurrent.ConcurrentHashMap[Int, WebSocket]

  def notify(msg: String) = sockets.values.foreach { s =>
    if(s.channel.isActive) s.send(msg)
  }

  object WebSockets {
    val intent: unfiltered.netty.websockets.Intent = {
      case _ => {
        case Open(s) =>
          logInfo(s"open a socket $executionCounter)")
          s.send("sys|hola!")
        case Message(s, Text(msg)) =>
          logInfo(s"received message $msg from socket $s")
        case Close(s) =>
          logInfo(s"close socket $s")
        case Error(s, e) =>
          e.printStackTrace

      }


    }
  }

  object WebServer {
    val nbWriteIntent: unfiltered.netty.cycle.Plan.Intent = {
      case req@PUT(Path(Seg("notebooks" :: name :: Nil))) =>
        val id = req.parameterValues("id").headOption
        val overwrite = req.parameterValues("force").headOption.exists(_.toBoolean)
        val contents = Body.string(req)
        logDebug("Putting notebook: " + contents)
        val nb = mapper.readValue(contents, classOf[Notebook])
        try {
          nbm.save(id, name, nb, overwrite)
          PlainTextContent ~> Ok
        } catch {
          case _: NotebookExistsException => PlainTextContent ~> Conflict
        }

      case req @DELETE(Path(Seg("notebooks" :: name :: Nil))) =>
        val id = req.parameterValues("id").headOption
        try {
          nbm.deleteNotebook(id, name)
          PlainTextContent ~> Ok
        } catch {
          case e: Exception =>
            logError("Error deleting notebook %s".format(name), e)
            InternalServerError
        }
    }


    val nbReadIntent: unfiltered.netty.cycle.Plan.Intent = {
      case req@GET(Path(Seg("notebooks" :: name :: Nil))) =>
        val id = req.parameterValues("id").headOption
        // json is the only supported format
        getNotebook(id, name)

//      case req@POST(Path(Seg("notebooks" :: name :: Nil))) =>
      case req@(Path(Seg("new" :: Nil))) =>
        val notebook_id = nbm.newNotebook()
        val notebook_name = nbm.idToName(notebook_id)
        Redirect("/view/" + notebook_name + "?id=" + notebook_id)

      case req@(Path(Seg("copy" :: name :: Nil))) =>
        val id = req.parameterValues("id").headOption
        val newId = nbm.copyNotebook(id, name)
        Redirect("/view/" + nbm.idToName(newId) + "?id=" + newId)
    }

    val otherIntent: unfiltered.netty.cycle.Plan.Intent = {
      case GET(Path("/")) =>
        Redirect("/index.html")
//        ResponseHeader("Access-Control-Allow-Origin", "*" :: Nil) ~> ResponseString("here is the index page~")

    }

  }

  def getNotebook(id: Option[String], name: String) = {
    try {
      val response = for ((lastMod, name, data) <- nbm.getNotebook(id, name)) yield {
        JsonContent ~> ResponseHeader("Content-Disposition", "attachment; filename=\"%s.snb".format(name) :: Nil) ~> ResponseHeader("Last-Modified", lastMod :: Nil) ~> ResponseString(data) ~> Ok
      }
      response.getOrElse(PlainTextContent ~> ResponseString("Notebook not found.") ~> NotFound)
    } catch {
      case e: Exception =>
        logError("Error accessing notebook %s".format(name), e)
        InternalServerError
    }
  }




}

