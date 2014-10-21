resolvers ++= Seq(
  Classpaths.typesafeResolver,
  "digimead-maven" at "http://storage.googleapis.com/maven.repository.digimead.org/")

libraryDependencies <+= (sbtBinaryVersion in update, scalaBinaryVersion in update, baseDirectory) { (sbtV, scalaV, base) =>
  Defaults.sbtPluginExtra("org.digimead" % "sbt-site-manager" %
    scala.io.Source.fromFile(base / Seq("..", "version").mkString(java.io.File.separator)).mkString.trim, sbtV, scalaV) }
