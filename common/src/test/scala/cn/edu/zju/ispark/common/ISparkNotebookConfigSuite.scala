package cn.edu.zju.ispark.common

import org.scalatest.FunSuite

/**
 * Created by king on 15-6-4.
 */
class ISparkNotebookConfigSuite extends FunSuite {

  test("new Config should load the setting conf file") {
    sys.props.put("ISPARK_HOME", "/home/king/gits/ispark")
    val conf = new ISparkNotebookConfig()
    conf.printsettings()

  }

}
