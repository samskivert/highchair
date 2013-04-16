import sbt._
import Keys._

object HighchairBuild extends Build {

  val highSettings = Defaults.defaultSettings ++ seq(
    organization := "com.samskivert",
    name := "Highchair",
    description := "A set of modules for developing Google App Engine services/apps in Scala.",
    version := "0.0.5",
    scalaVersion := "2.10.0",
    crossScalaVersions := Seq("2.9.2"),
    scalacOptions += "-deprecation",
    libraryDependencies ++= GAE.dependencies ++ Seq(
      "org.specs2" %% "specs2" % "1.13" % "test"),
    parallelExecution in Test := false,

    // bits for publishing to Maven Central
    publishMavenStyle := true,
    // don't add the scala-tools repository to our POM
    pomIncludeRepository := { (repo: MavenRepository) => false },
    publishTo <<= (version) { v: String =>
      val root = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at root + "content/repositories/snapshots/")
      else Some("staging" at root + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / "credentials-sonatype"),
    homepage := Some(url("http://github.com/samskivert/highchair")),
    licenses += ("The (New) BSD License" ->
      url("http://www.opensource.org/licenses/bsd-license.php")),
    startYear := Some(2010),
    pomExtra :=
      <developers>
        <developer>
          <id>chrislewis</id>
          <name>Chris Lewis</name>
          <email>highchair-user@googlegroups.com</email>
        </developer>
        <developer>
          <id>samskivert</id>
          <name>Michael Bayne</name>
          <email>mdb@samskivert.com</email>
        </developer>
      </developers>
      <scm>
        <connection>scm:git:git://github.com/samskivert/highchair.git</connection>
        <url>http://github.com/samskivert/highchair</url>
      </scm>
  )

  lazy val root = Project("highchair", file("."), settings = highSettings).aggregate(
    ds, spec, remote)
  lazy val ds = Project("highchair-datastore", file("datastore"), settings = highSettings ++ seq(
    name := "Highchair Datastore",
    libraryDependencies += "joda-time" % "joda-time" % "1.6.2"
  )) dependsOn(spec % "test")
  lazy val spec = Project("highchair-spec", file("spec"), settings = highSettings ++ seq(
    name := "Highchair Spec",
    publishLocal := false,
    publish := false
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
