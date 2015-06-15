package cn.edu.zju.ispark.server.calculator

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import scala.concurrent.duration._

/**
 * Created by king on 15-6-12.
 */
class SparkCalculatorSuite(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FunSuiteLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  var calculator: ActorRef = _

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  override def beforeAll(): Unit = {
    calculator = system.actorOf(Props[SparkCalculator])
  }

  def evaluate(input: String, output: String): Unit = {
    calculator ! ExecuteRequest(1, input)
    val r = within(30 seconds) {
      expectMsgPF() {
        case ExecuteResponse(e) =>
          e
      }
    }
    assert(r.contains(output.trim))
  }

  test("send common math calculation code") {
    val input = "1 + 1".stripMargin
    val output = """res0: Int = 2""".stripMargin
    evaluate(input, output)
  }

  test("send complex spark code") {
    val input = """var v = 7
                  |sc.parallelize(1 to 10).map(x => v).collect().reduceLeft(_+_)
                  |v = 10
                  |sc.parallelize(1 to 10).map(x => v).collect().reduceLeft(_+_)
                """.stripMargin
    val output =
      """v: Int = 7
        |res0: Int = 70
        |v: Int = 10
        |res1: Int = 100
      """.stripMargin

    evaluate(input, output)
  }


}
