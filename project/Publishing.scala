import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype.SonatypeKeys._
import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype.autoImport.{sonatypeProfileName => _, _}

object Publishing {

  val githubProjectName = "kamon-graphite"

  val settings = Seq(

    pomIncludeRepository in Global := { _ =>
      false
    },

    publishTo in Global := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },

    credentials ++= (for {
      username <- Option(System.getenv().get("SONATYPE_USERNAME"))
      password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
    } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq,

    // Your profile name of the sonatype account. The default is the same with the organization value
    sonatypeProfileName := "au.com.agiledigital",

    // To sync with Maven central, you need to supply the following information:
    pomExtra in Global := {
      <url>https://github.com/agiledigital/{githubProjectName}</url>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          </license>
        </licenses>
        <scm>
          <connection>scm:git:github.com/agiledigital/{githubProjectName}</connection>
          <developerConnection>scm:git:git@github.com:agiledigital/{githubProjectName}</developerConnection>
          <url>github.com/agiledigital/{githubProjectName}</url>
        </scm>
        <developers>
          <developer>
            <id>oss@agiledigital.com.au</id>
            <name>Justin Smith</name>
          </developer>
          <developer>
            <id>daniel.spasojevic@gmail.com</id>
            <name>Daniel Spasojevic</name>
          </developer>
        </developers>
    },

    releaseProcess in Global:= Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommand("publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    )
  )
}