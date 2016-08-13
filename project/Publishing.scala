import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype.SonatypeKeys._
import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype.autoImport.{sonatypeProfileName => _, _}

object Publishing {

  val githubProjectName = "kamon-graphite"

  val settings = Seq(

    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),

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

    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      ReleaseStep(action = Command.process("publishSigned", _)),
      setNextVersion,
      commitNextVersion,
      ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
      pushChanges
    )
  )
}