import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "jem-web"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.reactivemongo" %% "play2-reactivemongo" % "0.8"
    //"play.modules.reactivemongo" %% "play2-reactivemongo" % "0.1-SNAPSHOT"  cross CrossVersion.full
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    resolvers += "sgodbillon" at "https://bitbucket.org/sgodbillon/repository/raw/master/snapshots/"
  )

}
