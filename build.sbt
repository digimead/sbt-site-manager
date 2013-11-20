//
// Copyright (c) 2012-2013 Alexey Aksenov ezh@ezh.msk.ru
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

name := "sbt-site-manager"

description := "SBT documentation manager."

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

organization := "org.digimead"

organizationHomepage := Some(url("http://digimead.org"))

homepage := Some(url("https://github.com/digimead/sbt-doc-manager"))

version <<= (baseDirectory) { (b) => scala.io.Source.fromFile(b / "version").mkString.trim }

// There is no "-Xfatal-warnings" because we have cross compilation against different Scala versions
scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-deprecation", "-Xcheckinit")

// http://vanillajava.blogspot.ru/2012/02/using-java-7-to-target-much-older-jvms.html
javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-source", "1.6", "-target", "1.6")

javacOptions in doc := Seq("-source", "1.6")

if (sys.env.contains("XBOOTCLASSPATH")) Seq(javacOptions += "-Xbootclasspath:" + sys.env("XBOOTCLASSPATH")) else Seq()

sbtPlugin := true

resourceGenerators in Compile <+=
  (resourceManaged in Compile, name, version) map { (dir, n, v) =>
    val file = dir / "version-%s.properties".format(n)
    val contents = "name=%s\nversion=%s\nbuild=%s\n".format(n, v, ((System.currentTimeMillis / 1000).toInt).toString)
    IO.write(file, contents)
    Seq(file)
  }

resolvers ++= Seq(
  "doc-mananger-digimead-maven" at "http://storage.googleapis.com/maven.repository.digimead.org/",
  Resolver.url("doc-mananger-typesafe-ivy-releases-for-online-crossbuild", url("http://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns),
  Resolver.url("doc-mananger-typesafe-ivy-snapshots-for-online-crossbuild", url("http://repo.typesafe.com/typesafe/ivy-snapshots/"))(Resolver.defaultIvyPatterns),
  Resolver.url("doc-mananger-typesafe-repository-for-online-crossbuild", url("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns),
  Resolver.url("doc-mananger-typesafe-shapshots-for-online-crossbuild", url("http://typesafe.artifactoryonline.com/typesafe/ivy-snapshots/"))(Resolver.defaultIvyPatterns))

sourceGenerators in Compile <+= (sbtVersion, sourceDirectory in Compile, sourceManaged in Compile) map { (v, sourceDirectory, sourceManaged) =>
  val interface = v.split("""\.""").take(2).mkString(".")
  val source = sourceDirectory / ".." / "patch" / interface
  val generated = (PathFinder(source) ***) x Path.rebase(source, sourceManaged)
  IO.copy(generated, true, false)
  generated.map(_._2).filter(_.getName endsWith ".scala")
}

libraryDependencies += "org.digimead" %% "booklet-library" % "0.1.0.99-SNAPSHOT"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.5"
