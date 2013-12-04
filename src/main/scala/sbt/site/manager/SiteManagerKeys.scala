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

import sbt.Keys._

import sbt._

object SiteManagerKeys {
  def SiteConf = config("site") hide

  lazy val siteBlocks = TaskKey[Seq[SiteManagerPlugin.GenericBlock]]("siteBlocks", "Defines the mappings from a file to a path per site block.")
  lazy val siteExportBookletApp = TaskKey[Unit]("siteExportBookletApp", "Export booklet configuration and display starting hint.")
  lazy val siteExportBookletTemplates = TaskKey[Unit]("siteExportBookletTemplates", "Export template for all booklet blocks.")
  lazy val siteShowBrief = TaskKey[Unit]("siteShowBrief", "Show brief information about the site manager configuration.")
  lazy val siteShowDetailed = TaskKey[Unit]("siteShowDetailed", "Show detailed information about the site manager configuration.")
}
