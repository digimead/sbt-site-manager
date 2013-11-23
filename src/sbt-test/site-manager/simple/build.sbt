import sbt.site.manager._

SiteManager

name := "Simple"

version := "0.0.1.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

logLevel := Level.Debug

siteBlocks in SiteManagerConf <+= siteMappingForBooklet()
