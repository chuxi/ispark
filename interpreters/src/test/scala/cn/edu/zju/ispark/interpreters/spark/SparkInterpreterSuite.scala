package cn.edu.zju.ispark.interpreters.spark

import java.util.Properties

import cn.edu.zju.ispark.interpreters.InterSuccess
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

  def assertDoesNotContain(message: String, output: String) {
    val isContain = output.contains(message)
    assert(!isContain,
      "Interpreter output contained '" + message + "':\n" + output)
  }



  test("open a spark interpreter") {
    val rs = si.interpret("""
                        |var v = 7
                        |sc.parallelize(1 to 10).map(x => v).collect().reduceLeft(_+_)
                        |v = 10
                        |sc.parallelize(1 to 10).map(x => v).collect().reduceLeft(_+_)
                      """.stripMargin).asInstanceOf[InterSuccess].msg
    val rs1 = si.interpret("""
                           |v = 20
                           |sc.parallelize(1 to 10).map(x => v).collect().reduceLeft(_+_)
                         """.stripMargin).asInstanceOf[InterSuccess].msg
    assertDoesNotContain("error:", rs)
    assertDoesNotContain("Exception", rs)
    assertContains("res0: Int = 70", rs)
    assertContains("res1: Int = 100", rs)
    assertContains("res2: Int = 200", rs1)

  }

  override def afterAll(): Unit = {
    si.close()
  }

}
