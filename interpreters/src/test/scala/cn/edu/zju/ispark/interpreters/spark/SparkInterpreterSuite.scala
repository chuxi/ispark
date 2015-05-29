package cn.edu.zju.ispark.interpreters.spark

import java.util.Properties

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FunSuite}

/**
 * Created by king on 15-5-28.
 */
class SparkInterpreterSuite extends FunSuite with BeforeAndAfterAll{
  var si: SparkInterpreter = _

  override def beforeAll(): Unit = {
    si = new SparkInterpreter(new Properties())
    si.open()
  }



  test("open a spark interpreter") {
    val input = """
                  |val accum = sc.accumulator(0)
                  |sc.parallelize(1 to 10).foreach(x => accum += x)
                  |accum.value
                """.stripMargin
    si.interpret(input)
    println(si.out.toString)
  }

  override def afterAll(): Unit = {
    si.close()
  }

}
