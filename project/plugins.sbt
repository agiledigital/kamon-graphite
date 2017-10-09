// Coverage plugin
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// For publishing to coveralls
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")

// Wart removers
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.2.1")

// For performing releases
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.0")

// For formatting of the source code.
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.12")
