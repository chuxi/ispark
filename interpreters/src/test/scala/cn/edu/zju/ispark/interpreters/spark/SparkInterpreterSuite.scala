package cn.edu.zju.ispark.interpreters.spark

import java.util.Properties

import cn.edu.zju.ispark.interpreters.{InterError, InterIncomplete, InterSuccess}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

/**
 * Created by king on 15-5-28.
 */
class SparkInterpreterSuite extends FunSuite with BeforeAndAfterAll{
  var si: SparkInterpreter = _

  override def beforeAll(): Unit = {
    si = new SparkInterpreter(new Properties())
    si.open()
  }

  def assertContains(message: String, output: String) {
    val isContain = output.contains(message)
    assert(isContain,
      "Interpreter output did not contain '" + message + "':\n" + output)
  }

  def interpreterCode(input: String, testmsg: List[String] = List()): Unit = {
    val rs = si.interpret(input)
    rs match {
      case r: InterSuccess =>
        testmsg.foreach(assertContains(_, r.msg))
        println(r.msg)
      case r: InterIncomplete =>
        assertContains("Incomplete expression", r.msg)
        println("incomplete expr")
      case r: InterError =>
        assert(testmsg.isEmpty, "Got an Interpreter Error with input: " + input)
        println("error interpreter: " + r.msg)
    }
  }


  test("open a spark interpreter") {
    val input0 = """
                   |var v = 7
                   |sc.parallelize(1 to 10).map(x => v).collect().reduceLeft(_+_)
                   |v = 10
                   |sc.parallelize(1 to 10).map(x => v).collect().reduceLeft(_+_)
                 """.stripMargin

    val input1 = """
                   |v = 20
                   |sc.parallelize(1 to 10).map(x => v).collect().reduceLeft(_+_)
                 """.stripMargin

    interpreterCode(input0, List("res0: Int = 70", "res1: Int = 100"))

    interpreterCode(input1, List("res2: Int = 200"))
  }

  test("interpreter incomplete") {
    val input0 =
      """
        |val a = 1 until 4
        |for ( i <- a) {
        |   sc.parallelize(1 to 10).map(x => i).collect()
        |}
      """.stripMargin

    val input1 =
      """
        |val a = 1 until 4
        |for ( i <- a) {
        |   sc.parallelize(1 to 10).map(x => i).collect()
      """.stripMargin

    interpreterCode(input0, List())
    interpreterCode(input1, List())
  }

  test("test Error code") {
    val input0 =
      """
        |val a = 1
        |a = 2
      """.stripMargin
    interpreterCode(input0, List())
  }

  override def afterAll(): Unit = {
    si.close()
  }

}
