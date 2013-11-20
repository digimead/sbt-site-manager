/**
 * sbt-site-manager - SBT documentation manager.
 *
 * Copyright (c) 2013 Alexey Aksenov ezh@ezh.msk.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbt.site.manager

import java.util.Properties

import org.digimead.booklet.Settings
import org.digimead.booklet.template.BookletStorage
import org.digimead.booklet.template.Produce

import sbt.Keys._
import sbt.site.manager.Keys._
import sbt.std.TaskStreams

import sbt._

object Plugin {

  val bookletVersion = "0.1.0.99-SNAPSHOT"

  /** Name of the directory where composed site is located. */
  val siteComposedDirectoryName = "composed"
  /** Name of the directory where per block information is located. */
  val siteBlocksDirectoryName = "blocks"
  /** Entry point for plugin in user's project */
  lazy val defaultSettings = inConfig(SiteConf)(Seq(
    compile <<= compileTask,
    siteExportBookletApp <<= siteExportBookletAppTask,
    siteExportBookletTemplates <<= siteExportBookletTemplatesTask,
    siteBlocks := Seq(),
    // for example: siteBlocks <+= siteMappingForScalaDoc(),
    // for example: siteBlocks <+= siteMappingForBooklet(),
    // for example: siteBlocks <+= siteMappingForBooklet(input = _ / "myBooklet"),
    siteShowBrief <<= siteShowTask(false),
    siteShowDetailed <<= siteShowTask(true),
    target <<= (target in Compile)(_ / "site")))

  /** Generate booklet site block. */
  def booklet(block: BookletBlock, bookletProperties: Properties)(implicit arg: Plugin.TaskArgument) = {
    import Support._
    val output = block.output / siteBlocksDirectoryName / block.blockId.name
    arg.log.info(logPrefix(arg.name) + s"Transform '${block.blockId.name}': " +
      s"${relativeTo(block.base)(block.input) getOrElse block.input} -> ${relativeTo(block.base)(output) getOrElse output}.")
    if (!bookletProperties.isEmpty())
      arg.log.info(logPrefix(arg.name) + s"Block '${block.blockId.name}' properties are: \n" + bookletProperties)
    val cacheFile = block.output / siteBlocksDirectoryName / (block.blockId.name + ".build.cache")
    val cached = FileFunction.cached(cacheFile, FilesInfo.hash) { _ ⇒
      IO.delete((output ** AllPassFilter --- output).get)
      arg.log.debug(logPrefix(arg.name) + s"Produce and store content to " + output)
      Produce(BookletStorage(block.input, bookletProperties).globalized, output)
      (output ** AllPassFilter --- output).get.toSet
    }
    val cacheInputs = (block.input ** AllPassFilter).get.toSet
    /*
     * We must append 'block.input' to input argument
     *   because if cacheInputs is empty then cache is not updated
     * We must remove 'block.input' from output
     *   because 'block.input' havn't any mapping
     */
    cached(cacheInputs + block.input) --- block.input x relativeTo(output)
  }
  def compileTask = (siteBlocks in SiteConf, baseDirectory, state, streams, thisProjectRef) map { (siteBlocks, baseDirectory, state, streams, thisProjectRef) ⇒
    import Support._
    implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
    arg.log.info(logPrefix(arg.name) + s"Compose site from [${siteBlocks.map(_.blockId.name).sorted.mkString(", ")}]")
    for (block ← siteBlocks.sortBy(_.blockId.name))
      block.siteUpdate()
    inc.Analysis.Empty
  }
  def siteExportBookletAppTask = (appConfiguration, target in Compile, state, streams, thisProjectRef) map {
    (appConfiguration, target, state, streams, thisProjectRef) ⇒
      import Support._
      implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
      val launcher = try new File(appConfiguration.provider().scalaProvider().launcher().
        getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
      catch { case e: Throwable ⇒ new File("sbt-launch.jar") }
      val configuration = ("""[scala]
       |  version: 2.10.3
       |[app]
       |  org: org.digimead
       |  name: booklet-app
       |  version: """ + bookletVersion + """
       |  class: org.digimead.booklet.Application
       |  cross-versioned: binary
       |[repositories]
       |  local
       |  maven-central
       |  digimead-maven: http://storage.googleapis.com/maven.repository.digimead.org/
       |[boot]
       |  directory: ${user.home}/.sbt/booklet-boot""").stripMargin.split("\n")
      val configurationLocation = target / "booklet-app.configuration"
      IO.writeLines(configurationLocation, configuration)
      arg.log.info(logPrefix(arg.name) + s"java -Dsbt.boot.properties=${configurationLocation} -jar ${launcher} -h")
  }
  def siteExportBookletTemplatesTask = (siteBlocks in SiteConf, baseDirectory, state, streams, thisProjectRef) map {
    (siteBlocks, baseDirectory, state, streams, thisProjectRef) ⇒
      import Support._
      implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
      for (block ← siteBlocks.sortBy(_.blockId.name))
        block match {
          case block: BookletBlock ⇒
            arg.log.info(logPrefix(arg.name) + s"Export booklet template for '${block.blockId.name}' block.")
            val storage = BookletStorage(block.input)
            implicit val implicitProperties = storage.baseBookletProperties
            val userTemplatePath = new File(storage.input, Settings.template)
            Settings.languages(implicitProperties).foreach { lang ⇒
              val baseLang = new File(storage.input, lang)
              val userTemplatePathLang = if (lang == Settings.defaultLanguage) userTemplatePath else new File(baseLang, Settings.template)
              storage.writeTemplates(userTemplatePathLang, lang)
            }
            arg.log.info(logPrefix(arg.name) + s"Wrote templates for '${block.blockId.name}' to " + userTemplatePath)
          case block ⇒
        }
  }
  def siteShowTask(detailed: Boolean) = (siteBlocks in SiteConf, baseDirectory, state, streams, thisProjectRef) map {
    (siteBlocks, baseDirectory, state, streams, thisProjectRef) ⇒
      import Support._
      implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
      arg.log.info("Site properties of %s/%s".format(arg.name, arg.thisProjectRef.project))
      for (block ← siteBlocks.sortBy(_.blockId.name)) {
        val key = if (arg.log.ansiCodesSupported)
          s"Site block ${scala.Console.WHITE}'${block.blockId.name}'${scala.Console.GREEN} from"
        else
          s"Site block '${block.blockId.name}' from"
        val from = block match {
          case block: BookletBlock ⇒ relativeTo(baseDirectory)(block.input) getOrElse block.input
          case block: MappingBlock ⇒ "SBT mapping"
          case block ⇒ "user defined"
        }
        val value = from + " -> " + block.nestedDirectory.map("/" + _).getOrElse("/")
        show(key, value, scala.Console.CYAN)
        if (detailed) {
          block.mapping match {
            case Nil ⇒
              arg.log.info(logPrefix(arg.name) + "There are no files.")
            case mapping ⇒
              arg.log.info(logPrefix(arg.name) + s"'${block.blockId.name}' content:\n" +
                mapping.filterNot(_._1.isDirectory()).map(_._2).sorted.mkString("\n"))
          }
        }
      }
      () // Project/Def.Initialize[Task[Unit]]
  }

  /** Build booklet site block with default settings. */
  def siteMappingForBooklet(id: Symbol = 'Booklet,
    input: File ⇒ File = _ / "docs",
    nestedDirectory: Option[String] = None,
    bookletProperties: Properties = new Properties,
    userMapping: Seq[(File, String)] ⇒ Seq[(File, String)] = n ⇒ n) =
    (baseDirectory, target in SiteConf, state, streams, thisProjectRef) map { (baseDirectory, target, state, streams, thisProjectRef) ⇒
      implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
      new BookletBlock(baseDirectory, id, booklet(_, bookletProperties), input(baseDirectory), nestedDirectory, target, userMapping)
    }
  /** Build Scala doc site block with default settings. */
  def siteMappingForScalaDoc(id: Symbol = 'ScalaDoc,
    nestedDirectory: Option[String] = Some("api"),
    mappings: TaskKey[Seq[(File, String)]] = mappings in packageDoc in Compile,
    userMapping: Seq[(File, String)] ⇒ Seq[(File, String)] = n ⇒ n) =
    (mappings, target in SiteConf) map { (m, site) ⇒ new MappingBlock(id, nestedDirectory, m, site, userMapping) }

  /**
   * Container for booklet site block.
   *
   * @param base base project directory
   * @param blockId unique block Id
   * @param booklet booklet generation function
   * @param input directory with raw content and template
   * @param nestedDirectory directory of content within site
   * @param output target directory
   */
  class BookletBlock(val base: File, blockId: Symbol, val booklet: BookletBlock ⇒ Seq[(File, String)],
    val input: File, nestedDirectory: Option[String], output: File, userMapping: Seq[(File, String)] ⇒ Seq[(File, String)] = n ⇒ n)
    extends GenericBlock(blockId, nestedDirectory, output) {
    def mapping: Seq[(File, String)] = userMapping(for ((f, d) ← booklet(this)) yield (f, nestedDirectory.map(_ + "/" + d).getOrElse(d)))
    def siteUpdate()(implicit arg: Plugin.TaskArgument) = {
      arg.log.info(Support.logPrefix(arg.name) + s"Update '${blockId.name}' block")
      val mapping = this.mapping
      val actualMapping = (mapping.map(_._1), mapping.map(output / siteComposedDirectoryName / _._2)).zipped
      Sync(output / siteBlocksDirectoryName / (blockId.name + ".update.cache"))(actualMapping)
      actualMapping
    }
  }
  /**
   * Container for SBT mapping site block.
   *
   * @param blockId unique block Id
   * @param nestedDirectory directory of content within site
   * @param origin SBT mapping
   * @param output target directory
   */
  class MappingBlock(blockId: Symbol, nestedDirectory: Option[String], origin: ⇒ Seq[(File, String)],
    output: File, userMapping: Seq[(File, String)] ⇒ Seq[(File, String)] = n ⇒ n)
    extends GenericBlock(blockId, nestedDirectory, output) {
    def mapping = userMapping(for ((f, d) ← origin) yield (f, nestedDirectory.map(_ + "/" + d).getOrElse(d)))
    def siteUpdate()(implicit arg: Plugin.TaskArgument): Traversable[(File, File)] = {
      arg.log.info(Support.logPrefix(arg.name) + s"Update '${blockId.name}' block")
      val actualMapping = (origin.map(_._1), mapping.map(output / siteComposedDirectoryName / _._2)).zipped
      Sync(output / siteBlocksDirectoryName / (blockId.name + ".update.cache"))(actualMapping)
      actualMapping
    }
  }
  /**
   * Container for generic site block.
   *
   * @param blockId unique block Id
   * @param nestedDirectory directory of content within site
   * @param output target directory
   */
  abstract class GenericBlock(val blockId: Symbol, val nestedDirectory: Option[String], val output: File) {
    def mapping: Seq[(File, String)]
    def siteUpdate()(implicit arg: Plugin.TaskArgument): Traversable[(File, File)]
  }

  object Support {
    /** Default sbt-site-manager log prefix */
    def logPrefix(name: String) = "[Site manager:%s] ".format(name)

    /** Display the single property */
    def show(parameter: String, value: AnyRef, onEmpty: String)(implicit arg: Plugin.TaskArgument): Unit =
      show(parameter, value, Some("", onEmpty))(arg)
    /** Display the single property */
    def show(parameter: String, value: AnyRef, onEmpty: String, color: String)(implicit arg: Plugin.TaskArgument): Unit =
      show(parameter, value, Some(color, onEmpty))(arg)
    /** Display the single property */
    def show(parameter: String, value: AnyRef)(implicit arg: Plugin.TaskArgument): Unit =
      show(parameter, value, None)(arg)
    /** Display the single property */
    def show(parameter: String, value: AnyRef, onEmpty: Option[(String, String)])(implicit arg: Plugin.TaskArgument) {
      val key = if (arg.log.ansiCodesSupported) scala.Console.GREEN + parameter + scala.Console.RESET else parameter
      val message = onEmpty match {
        case Some((color, message)) ⇒
          if (Option(value).isEmpty || value.toString.trim.isEmpty) {
            if (arg.log.ansiCodesSupported)
              key + ": " + color + message + scala.Console.RESET
            else
              key + ": " + message
          } else
            key + ": " + value.toString()
        case None ⇒
          if (Option(value).isEmpty || value.toString.trim.isEmpty) return
          key + ": " + value.toString()
      }
      arg.log.info(logPrefix(arg.name) + message)
    }
  }

  /** Consolidated argument with all required information */
  case class TaskArgument(
    /** The data structure representing all command execution information. */
    state: State,
    // It is more reasonable to pass it from SBT than of fetch it directly.
    /** The reference to the current project. */
    thisProjectRef: ProjectRef,
    /** The structure that contains reference to log facilities. */
    streams: Option[TaskStreams[ScopedKey[_]]] = None) {
    /** Extracted state projection */
    lazy val extracted = Project.extract(state)
    /** SBT logger */
    val log = streams.map(_.log) getOrElse {
      // Heh, another feature not bug? SBT 0.12.3
      // MultiLogger and project level is debug, but ConsoleLogger is still info...
      // Don't care about CPU time
      val globalLoggin = _root_.sbt.site.manager.patch.Patch.getGlobalLogging(state)
      import globalLoggin._
      full match {
        case logger: AbstractLogger ⇒
          val level = logLevel in thisScope get extracted.structure.data
          level.foreach(logger.setLevel(_)) // force level
          logger
        case logger ⇒
          logger
      }
    }
    /** Current project name */
    val name: String = (_root_.sbt.Keys.name in thisScope get extracted.structure.data) getOrElse thisProjectRef.project.toString()
    /** Scope of current project */
    lazy val thisScope = Load.projectScope(thisProjectRef)
    /** Scope of current project withing plugin configuration */
    lazy val thisDocScope = thisScope.copy(config = Select(SiteConf))
  }
}
