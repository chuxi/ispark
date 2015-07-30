package cn.edu.zju.ispark.server

import java.util.concurrent.CountDownLatch

import cn.edu.zju.ispark.common.ISparkNotebookConfig
import org.apache.http.client.methods.{HttpGet, HttpRequestBase}
import org.apache.http.impl.client.HttpClients
import org.scalatest.{BeforeAndAfterAll, FunSuite}

/**
 * Created by king on 15-6-3.
 */
class DispatcherSuite extends FunSuite with BeforeAndAfterAll {

  val ServerPort = 8080
  val WebSocketPort = 8081
  val ServerHost = "127.0.0.1"


  @volatile var server: unfiltered.netty.Server = _
  @volatile var serverThread: Thread = _
  @volatile var dispatcher: Dispatcher = _

  override def beforeAll(): Unit = {
    // simulate a http and websocket server by thread
    val serverStarted = new CountDownLatch(1)

    serverThread = new Thread(new Runnable {

      override def run(): Unit = {
        try {
          sys.props.put("ISPARK_HOME", "/home/king/gits/ispark")
          Server.startServer(Array(), new ISparkNotebookConfig()){
            (http, app) =>
              server = http
              dispatcher = app
              serverStarted.countDown()}
          println("hello world")
        } catch {
          case e: InterruptedException => println("server finished")
        }

      }
    })
    serverThread.setName("main")
    serverThread.start()

    serverStarted.await()
  }

  override def afterAll(): Unit = {
    server.stop()
    serverThread.interrupt()
  }

  val testnb = Notebook(new Metadata("sample"), List(Worksheet(List(CodeCell("1+2", "python", false, Some(2), List(ScalaOutput(2, None, Some("3"))))))), Nil, None)


  /**
   * HTTP connection test
   */

  def serverURL(path: String = "/") = "http://%s:%d%s".format(ServerHost, ServerPort, path)

  def httpGet(path: String): HttpRequestBase ={
    val client = HttpClients.createDefault()
    new HttpGet(serverURL(path))
  }

  def assertResponseCode(code: Int, req: HttpRequestBase) = {
    val client = HttpClients.createDefault()

    val response = client.execute(req)
//    println(response.toString)
//    println(response.getEntity)
    assert (code === response.getStatusLine.getStatusCode, "Expected %d code for resource '%s' but response was (%d: %s)".format(code, req.getURI, response.getStatusLine.getStatusCode, response.getStatusLine.getReasonPhrase))
  }


  test("test") {
//    val indexGet = httpGet("/")
    assertResponseCode(200, httpGet("/"))
  }



}
