// The scala style plugin
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

// Coverage plugin
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")

// For publishing to coveralls
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")

// Wart removers
addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % "0.14")

// For performing releases
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

// For formatting of the source code.
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")
