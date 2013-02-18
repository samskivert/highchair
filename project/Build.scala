import sbt._
import Keys._

object HighchairBuild extends Build {

  val highSettings = Defaults.defaultSettings ++ seq(
    organization := "net.thegodcode",
    name := "Highchair",
    version := "0.0.5-SNAPSHOT",
    scalaVersion := "2.10.0",
    crossScalaVersions := Seq("2.9.2"),
    scalacOptions += "-deprecation",
    libraryDependencies ++= GAE.dependencies ++ Seq(
      "org.specs2" %% "specs2" % "1.13" % "test"),
    parallelExecution in Test := false,
    resolvers := Seq(ScalaToolsSnapshots),
    publishTo <<= (version) { version: String =>
      val nexus = "http://nexus.scala-tools.org/content/repositories/"
      if (version.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus+"snapshots/")
      else
        Some("releases" at nexus+"releases/")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  )

  lazy val root = Project("highchair", file("."), settings = highSettings).aggregate(
    ds, spec, remote)
  lazy val ds = Project("highchair-datastore", file("datastore"), settings = highSettings ++ seq(
    name := "Highchair Datastore",
    libraryDependencies += "joda-time" % "joda-time" % "1.6.2"
  )) dependsOn(spec % "test")
  lazy val spec = Project("highchair-spec", file("spec"), settings = highSettings ++ seq(
    name := "Highchair Spec"
  ))
  lazy val remote = Project("highchair-remote", file("remote"), settings = highSettings ++ seq(
    name := "Highchair Remote"
  ))
}

object GAE {
  val gae_version = "1.7.5"
  val group = "com.google.appengine"

  def artifact(id: String) = group % id % gae_version % "provided"

  val sdk     = artifact("appengine-api-1.0-sdk")
  val remote  = artifact("appengine-remote-api")
  val stubs   = artifact("appengine-api-stubs")
  val test    = artifact("appengine-testing")
  val labsApi = artifact("appengine-api-labs")

  val dependencies = Seq(sdk, remote, stubs, test, labsApi)
}
