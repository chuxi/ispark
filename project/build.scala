import sbt.Build
import sbt._
import sbt.Keys._

object ISparkBuild extends Build {


  override def settings = super.settings ++ Seq(
    organization := "cn.edu.zju",
    name := "ispark",
    version := "0.1.0",
    scalaVersion := "2.10.5",
    fork in Test in ThisBuild := true,
    parallelExecution in Test in ThisBuild := false,
    javaOptions in ThisBuild ++= Seq("-Xmx256M", "-XX:MaxPermSize=128M")
  )

  lazy val root = Project(id = "ispark", base = file("."))
    .aggregate(server)


  lazy val server = Project(id = "server", base = file("server"))
    .settings(
      name := "ispark-server"
    )

  object Dependencies {
    val unfilteredVersion     = "0.9.0-beta1"
    val akkaVersion           = "2.3.11"
    val unfilteredFilter      = "net.databinder"            %%      "unfiltered-filter"      % unfilteredVersion
    val unfilteredWebsockets  = "net.databinder"            %% "unfiltered-netty-websockets" % unfilteredVersion
    val unfilteredJson        = "net.databinder"            %%       "unfiltered-json"       % unfilteredVersion
  }


}

