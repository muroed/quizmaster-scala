// // repository for Typesafe plugins
resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

// Tell the compiler to ignore version mismatches in scala-xml
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.12") // Must be the same as BuildSettings.versions.play
// addSbtPlugin("com.vmunier" % "sbt-play-scalajs" % "0.3.1")

// scala.js plugins
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0")

// Web plugins
addSbtPlugin("com.github.sbt" % "sbt-less" % "2.0.1")
addSbtPlugin("com.github.sbt" % "sbt-rjs" % "2.0.0")
addSbtPlugin("com.github.sbt" % "sbt-digest" % "2.1.0")
addSbtPlugin("com.github.sbt" % "sbt-mocha" % "2.1.0")
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.21.1")

// Other
addSbtPlugin("com.github.sbt" % "sbt-gzip" % "2.0.0")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.13.0")
// addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.4.0")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
