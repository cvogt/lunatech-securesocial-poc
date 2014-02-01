import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "securesocial-poc"
  val appVersion      = "1.1-SNAPSHOT"

  val appDependencies = Seq(
    jdbc,
    "com.typesafe.slick" %% "slick" % "2.0.0",
    "com.h2database" % "h2" % "1.3.166",
    "ws.securesocial" %% "securesocial" % "2.1.3"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += Resolver.sonatypeRepo("releases")
  )

}
