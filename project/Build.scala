import sbt._
import Keys._

object HighchairBuild extends Build {
  lazy val root = Project("highchair", file("."), settings = Common.settings)
    .aggregate(datastore, spec, remote, util)
  lazy val datastore = Project("highchair-datastore", file("datastore"),
    settings = Common.settings ++ Seq(
      name := "Highchair Datastore",
      libraryDependencies += "joda-time" % "joda-time" % "1.6.2"
    )
  ) dependsOn(spec % "test")
  lazy val spec = Project("highchair-spec", file("spec"),
    settings = Common.settings ++
      Seq(
        name := "Highchair Spec",
        libraryDependencies <++= scalaVersion(v => Seq(Common.specsDep(v)("compile")))
      )
  ) dependsOn(util)
  lazy val remote = Project("highchair-remote", file("remote"),
    settings = Common.settings :+
      (name := "Highchair Remote")
  )
  lazy val util = Project("highchair-util", file("util"),
    settings = Common.settings ++ Seq(
      name := "Highchair Util",
      libraryDependencies += Common.dispatchDep("test")
    )
  )
}

object GAE {
  val gae_version = "1.6.2.1"
  val group = "com.google.appengine"

  def artifact(id: String) = group % id % gae_version % "provided"

  val sdk     = artifact("appengine-api-1.0-sdk")
  val remote  = artifact("appengine-remote-api")
  val stubs   = artifact("appengine-api-stubs")
  val test    = artifact("appengine-testing")
  val labsApi = artifact("appengine-api-labs")

  val dependencies = Seq(sdk, remote, stubs, test, labsApi)
}

object Common {

  val dispatch_version = "0.8.7"

  def dispatchDep(cfg: String) =
    "net.databinder" %% "dispatch-http" % dispatch_version % cfg

  def specsDep(sv: String)(cfg: String) =
    sv.substring(0, 3) match {
      case "2.8" => "org.scala-tools.testing" % "specs_2.8.1" % "1.6.8" % cfg
      case "2.9" => "org.scala-tools.testing" %% "specs" % "1.6.9" % cfg
      case _ => error("unsupported")
    }

  val settings = Defaults.defaultSettings ++ Seq(
    organization := "net.thegodcode",
    name := "Highchair",
    version := "0.0.5-SNAPSHOT",
    scalaVersion := "2.9.1",
    crossScalaVersions := Seq("2.8.1", "2.9.0-1"),
    scalacOptions += "-deprecation",
    libraryDependencies ++= GAE.dependencies ++ Seq(
      "org.specs2" %% "specs2" % "1.8.2" % "test"),
    libraryDependencies <+= scalaVersion(v => specsDep(v)("test")),
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
}
