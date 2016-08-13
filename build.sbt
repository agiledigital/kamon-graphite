import org.scalastyle.sbt.ScalastylePlugin._

val Specs2Version = "3.6.5"

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

lazy val commonSettings = Seq(
  organization := "au.com.agiledigital",
  scalaVersion := "2.11.8",
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-encoding", "UTF-8"),
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xlint", // Enable recommended additional warnings.
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
    "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
    "-Ywarn-numeric-widen" // Warn when numerics are widened.
  ),
  libraryDependencies ++= Seq(
    "io.dropwizard.metrics" % "metrics-graphite" % "3.1.2",
    "io.kamon" %% "kamon-core" % "0.6.2"
  ),
  // Disable scaladoc generation in dist.
  sources in(Compile, doc) := Seq.empty,
  updateOptions := updateOptions.value.withCachedResolution(true),
  // Restrict resources that will be used.
  concurrentRestrictions in Global := Seq(
    Tags.limitSum(1, Tags.Compile, Tags.Test),
    Tags.limitAll(2)
  ),
  scalastyleFailOnError := true,
  compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value,
  (test in Test) <<= (test in Test) dependsOn compileScalastyle,
  wartremoverErrors in (Compile, compile) ++= Seq(
        Wart.FinalCaseClass,
        Wart.Null,
        Wart.TryPartial,
        Wart.OptionPartial,
        Wart.ListOps,
        Wart.EitherProjectionPartial,
        Wart.Any2StringAdd,
        Wart.AsInstanceOf,
        Wart.ExplicitImplicitTypes,
        Wart.MutableDataStructures,
        Wart.Return,
        Wart.AsInstanceOf,
        Wart.IsInstanceOf)
) ++ Formatting.formattingSettings ++ Publishing.settings

lazy val kamonGraphite = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "kamon-graphite"
  )
