import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "jem-web"
  val appVersion      = "0.1-SNAPSHOT"

  val appDependencies = Seq(
    //"org.reactivemongo" %% "reactivemongo" % "0.9-scalafuture-leak-fix-SNAPSHOT"
    //"org.reactivemongo" %% "play2-reactivemongo" % "0.8"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
  )

}
