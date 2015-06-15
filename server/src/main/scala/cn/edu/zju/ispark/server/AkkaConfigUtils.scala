package cn.edu.zju.ispark.server

import com.typesafe.config.{ConfigFactory, Config}
import scala.collection.JavaConversions._

/**
 * Created by king on 15-6-3.
 */
object AkkaConfigUtils {

  private val SecureCookiePath = "akka.remote.secure-cookie"
  private val RequireCookiePath = "akka.remote.require-cookie"

  /** Creates a configuration that requires the specified secure requiredCookie if defined. */
  def requireCookie(baseConfig: Config, cookie: String) =
    ConfigFactory.parseMap(Map(
      SecureCookiePath -> cookie,
      RequireCookiePath -> "on"
    )).withFallback(baseConfig)

  /** If the specified configuration requires a secure requiredCookie, but does not define the requiredCookie value, this generates a new config with an appropriate value. */
  def optSecureCookie(baseConfig: Config, cookie: => String) =
    requiredCookie(baseConfig).map {
      req => if (req.isEmpty) {
        ConfigFactory.parseMap(Map(SecureCookiePath -> cookie)).withFallback(baseConfig)
      } else baseConfig
    } getOrElse baseConfig

  /** Returns the secure requiredCookie value if the specified Config requires their use. */
  def requiredCookie(config: Config) =
    if (config.getBoolean(RequireCookiePath)) {
      Some(config.getString(SecureCookiePath))
    } else None

}
