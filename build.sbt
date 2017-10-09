val Specs2Version = "4.0.0"

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

lazy val commonSettings = Seq(
  organization := "au.com.agiledigital",
  scalaVersion := "2.12.3",
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
    "io.kamon" %% "kamon-core" % "0.6.7" % "provided"
  ),
  // Disable scaladoc generation in dist.
  sources in (Compile, doc) := Seq.empty,
  updateOptions := updateOptions.value.withCachedResolution(true),
  // Restrict resources that will be used.
  concurrentRestrictions in Global := Seq(
    Tags.limitSum(1, Tags.Compile, Tags.Test),
    Tags.limitAll(2)
  ),
  wartremoverErrors in (Compile, compile) ++= Seq(
    Wart.FinalCaseClass,
    Wart.Null,
    Wart.TryPartial,
    Wart.Var,
    Wart.OptionPartial,
    Wart.TraversableOps,
    Wart.EitherProjectionPartial,
    Wart.StringPlusAny,
    Wart.AsInstanceOf,
    Wart.ExplicitImplicitTypes,
    Wart.MutableDataStructures,
    Wart.Return,
    Wart.AsInstanceOf,
    Wart.IsInstanceOf
  )
) ++ Publishing.settings

lazy val kamonGraphite = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "kamon-graphite"
  )
