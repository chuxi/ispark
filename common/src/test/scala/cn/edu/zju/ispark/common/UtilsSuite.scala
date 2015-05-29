package cn.edu.zju.ispark.common

import java.io.File
import scala.collection.JavaConversions._

import org.scalatest.FunSuite

/**
 * Created by king on 15-5-22.
 */
class UtilsSuite extends FunSuite {

  test("Load properties present in the given file: getPropertiesFromFile") {
    sys.props.put("ISPARK_HOME", "/home/king/gits/ispark")

    Utils.getPropertiesFromFile().foreach(println)
  }

}
