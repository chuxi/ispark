package cn.edu.zju.ispark.server


import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import cn.edu.zju.ispark.common.{ISparkNotebookConfig, Logging}
import cn.edu.zju.ispark.server.calculator.{ExecuteRequest, SessionRequest, InterruptCalculator}
import cn.edu.zju.ispark.server.kernel.{Kernel, KernelManager}
import com.typesafe.config.ConfigFactory
import org.json4s.JsonAST.{JString, JField, JObject}
import org.json4s.JsonDSL._
import org.json4s.NoTypeHints
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import unfiltered.netty.websockets._
import unfiltered.request._
import unfiltered.response._


/**
 * Created by king on 15-5-23.
 */
class Dispatcher(config: ISparkNotebookConfig) extends Logging {

  val executionCounter = new AtomicInteger(0)

  val nbm = new NotebookManager(config.projectName, config.notebooksDir)
  val system = ActorSystem("ISparkNotebookServer", AkkaConfigUtils.optSecureCookie(ConfigFactory.load("akka-server.conf"), akka.util.Crypt.generateSecureCookie))

  val kernelIdToCalcService = collection.mutable.HashMap[String, CalcWebSocketService]()

  implicit val formats = Serialization.formats(NoTypeHints)

  object WebSockets {
    val intent: unfiltered.netty.websockets.Intent = {
      case req@Path(Seg("kernels" :: kernelId :: channel :: Nil)) => {
        case Open(websock) =>
          for (calcService <- kernelIdToCalcService.get(kernelId)) {
            logInfo("Opening Socket " + channel + " for " + kernelId + " to " + websock)
            calcService.sparkPromise.success(new WebSockWrapperImpl(websock))
          }
        case Message(socket, Text(msg)) =>
          for (calcService <- kernelIdToCalcService.get(kernelId)) {
            logDebug("Message for " + kernelId + ":" + msg)
            // design the message formulation
            val json = parse(msg)

            for {
              JObject(obj) <- json
              JField("header", header) <- obj
              JField("session", session) <- header
              JField("msg_type", msgType) <- header
              JField("content", content) <- obj
            } {
              msgType match {
                case JString("execute_request") =>
                  for (JField("code", JString(code)) <- content) {
                    val execCounter = executionCounter.incrementAndGet()
                    calcService.calcActor ! SessionRequest(header, session, ExecuteRequest(execCounter, code))
                  }
                case x => logWarning("Unrecognized websocket message: " + msg)
              }
            }

          }
        case Close(s) =>
          logInfo(s"close socket $s")
          for (kernel <- KernelManager.get(kernelId)) {
            kernel.shutdown()
          }
        case Error(s, e) =>
          e.printStackTrace()

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

      case req@DELETE(Path(Seg("notebooks" :: name :: Nil))) =>
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
      case GET(Path("/hello")) =>
        Redirect("/index.html")
      case req@GET(Path("/about")) =>
        Redirect("/views/about.html")
//        ResponseHeader("Access-Control-Allow-Origin", "*" :: Nil) ~> ResponseString("here is the index page~")

    }

    // To every connection to the websocket, start a kernel for service
    val kernelIntent: unfiltered.netty.async.Plan.Intent = {
      case req@POST(Path(Seg("kernels" :: Nil))) =>
        logInfo("Starting kernel")
        req.respond(startKernel(UUID.randomUUID().toString))

      case req@POST(Path(Seg("kernels" :: kernelId :: "interrupt" :: Nil))) =>
        logInfo("Interrupting kernel " + kernelId)
        for (calcService <- kernelIdToCalcService.get(kernelId)) {
          calcService.calcActor ! InterruptCalculator
        }
        req.respond(PlainTextContent ~> Ok)
    }

    def startKernel(kernelId: String) = {
      val kernel = new Kernel(system)
      KernelManager.add(kernelId, kernel)
      val service = new CalcWebSocketService(system, List(), List(), kernel.remoteDeployFuture)
      kernelIdToCalcService += kernelId -> service
      val json = ("kernel_id" -> kernelId) ~ ("ws_url" -> "ws:/%s:%d".format(config.get("host"), config.getInt("port")))
      JsonContent ~> ResponseString(compact(render(json))) ~> Ok
    }

    def getNotebook(id: Option[String], name: String) = {
      try {
        val response = for ((lastMod, name, data) <- nbm.getNotebook(id, name)) yield {
          JsonContent ~> ResponseHeader("Content-Disposition", "attachment; filename=\"%s.snb".format(name) :: Nil) ~> ResponseHeader("Last-Modified", lastMod :: Nil) ~> ResponseString(write(data) ) ~> Ok
        }
        response.getOrElse(PlainTextContent ~> ResponseString("Notebook not found.") ~> NotFound)
      } catch {
        case e: Exception =>
          logError("Error accessing notebook %s".format(name), e)
          InternalServerError
      }
    }

  }

}

