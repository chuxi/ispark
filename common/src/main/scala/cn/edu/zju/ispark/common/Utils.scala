package cn.edu.zju.ispark.common

import java.io.{File, FileInputStream, IOException, InputStreamReader}
import java.util.Properties

import scala.collection.JavaConversions._
import scala.collection.Map

/**
 * Created by king on 15-5-22.
 */
object Utils extends Logging {


  /** Load properties present in the given file. */
  def getPropertiesFromFile(filename: String = null): Map[String, String] = {
    val file = if (filename != null) new File(filename) else new File(getConfigurationFile())
    require(file.exists(), s"Properties file $file does not exist")
    require(file.isFile, s"Properties file $file is not a normal file")

    val inReader = new InputStreamReader(new FileInputStream(file), "UTF-8")
    try {
      val properties = new Properties()
      properties.load(inReader)
      properties.stringPropertyNames().map(k => (k, properties(k).trim)).toMap
    } catch {
      case e: IOException =>
        throw new Exception(s"Failed when loading ISpark properties from $filename", e)
    } finally {
      inReader.close()
    }
  }

  def getConfigurationFile(props: Map[String, String] = sys.props): String = {
    props.get("ISPARK_HOME").map(t => s"$t${File.separator}conf")
      .map(t => new File(s"$t${File.separator}ispark.conf")).filter(_.isFile)
      .map(_.getAbsolutePath).orNull
  }


  /**
   * JSON mapperobject
   */
//  val mapper = {
//    val _mapper = new ObjectMapper()
//    _mapper.registerModule(DefaultScalaModule)
//    _mapper.enable(SerializationFeature.INDENT_OUTPUT)
//    //    _mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE)
//    _mapper
//  }




}
