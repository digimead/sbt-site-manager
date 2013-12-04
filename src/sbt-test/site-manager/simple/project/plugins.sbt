resolvers ++= Seq(
  Classpaths.typesafeResolver,
  "digimead-maven" at "http://storage.googleapis.com/maven.repository.digimead.org/")

addSbtPlugin("org.digimead" % "sbt-site-manager" % "0.1.0.101-SNAPSHOT")
