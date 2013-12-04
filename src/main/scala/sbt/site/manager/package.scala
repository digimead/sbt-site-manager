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

package sbt.site

import java.util.Properties

import sbt._
import sbt.Keys._

package object manager {
  /** Entry point for the plugin in user's project */
  def SiteManager = SiteManagerPlugin.defaultSettings

  // SSM SBT Site Manager
  // export declarations for consumer
  lazy val SSMKey = SiteManagerKeys
  lazy val SSMConf = SiteManagerKeys.SiteConf
  def siteBlocks = SiteManagerKeys.siteBlocks in SSMConf

  def siteMappingForScalaDoc(id: Symbol = 'ScalaDoc, nestedDirectory: Option[String] = Some("api"),
    mappings: TaskKey[Seq[(File, String)]] = mappings in packageDoc in Compile,
    userMapping: Seq[(File, String)] ⇒ Seq[(File, String)] = n ⇒ n) =
    SiteManagerPlugin.siteMappingForScalaDoc(id, nestedDirectory, mappings, userMapping)
  def siteMappingForBooklet(id: Symbol = 'Booklet, input: File ⇒ File = _ / "docs",
    nestedDirectory: Option[String] = None, bookletProperties: Properties = new Properties,
    userMapping: Seq[(File, String)] ⇒ Seq[(File, String)] = n ⇒ n) =
    SiteManagerPlugin.siteMappingForBooklet(id, input, nestedDirectory, bookletProperties, userMapping)
}
