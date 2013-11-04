import sbt.dependency.manager._

import sbt.site.manager._

DependencyManager

DocManager

name := "Simple"

version := "0.0.1.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

logLevel := Level.Info
