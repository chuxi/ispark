import sbt.Build
import sbt._
import sbt.Keys._

object ISparkBuild extends Build {
  import Dependencies._


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
    .aggregate(server, common)

  lazy val common = Project(id = "common", base = file("common"))
    .settings(
      name := "ispark-common",

      libraryDependencies ++= Seq(
        json4sjack,
        slf4jLog4j,
        scalatest
      )
    )


  lazy val server = Project(id = "server", base = file("server"))
    .dependsOn(common, interpreters)
    .settings(
      name := "ispark-server",
      updateOptions := updateOptions.value.withCachedResolution(true),

      libraryDependencies ++= Seq(
        akka,
        akkaRemote,
        akkaTestKit,
        commonIO,
        commonsHttp,
        commonsexec,
        json4sjack,
        unfilteredFilter,
        unfilteredWebsockets
      )

    )

  lazy val interpreters = Project(id = "interpreters", base = file("interpreters"))
    .dependsOn(common)
    .settings(
      name := "ispark-interpreters",
      updateOptions := updateOptions.value.withCachedResolution(true),

      libraryDependencies ++= Seq(
        sparkrepl,
        commonLang3,
        scalatest
      )
    )



  object Dependencies {
    val unfilteredVersion     = "0.9.0-beta1"
    val akkaVersion           = "2.3.11"
    val commonLang3           = "org.apache.commons"            %  "commons-lang3"                %      "3.4"
    val commonIO              = "commons-io"                    %  "commons-io"                   %      "2.4"
    val commonsHttp           = "org.apache.httpcomponents"     %  "httpclient"                   %      "4.4.1"
    val slf4jLog4j            = "org.slf4j"                     %  "slf4j-log4j12"                %      "1.7.12"
    val commonsexec           = "org.apache.commons"            %  "commons-exec"                 %       "1.3"
    val unfilteredFilter      = "net.databinder"                %% "unfiltered-filter"            % unfilteredVersion
    val unfilteredWebsockets  = "net.databinder"                %% "unfiltered-netty-websockets"  % unfilteredVersion
    val unfilteredJson        = "net.databinder"                %% "unfiltered-json"              % unfilteredVersion
    val akka                  = "com.typesafe.akka"             %% "akka-actor"                   %    akkaVersion
    val akkaRemote            = "com.typesafe.akka"             %% "akka-remote"                  %    akkaVersion
    val akkaTestKit           = "com.typesafe.akka"             %  "akka-testkit_2.10"            %    akkaVersion
    val scalatest             = "org.scalatest"                 %% "scalatest"                    %      "2.2.4"
    val sparkrepl             = "org.apache.spark"              %% "spark-repl"                   %      "1.3.1"
    val json4sjack            = "org.json4s"                    %  "json4s-jackson_2.10"          %     "3.3.0.RC2"
  }


}

