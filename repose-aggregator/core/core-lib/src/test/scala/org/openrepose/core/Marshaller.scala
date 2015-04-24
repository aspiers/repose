/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.openrepose.core

import java.net.URL
import java.nio.charset.StandardCharsets

import org.openrepose.commons.config.parser.jaxb.JaxbConfigurationParser
import org.openrepose.commons.config.resource.impl.{ByteArrayConfigurationResource, BufferedURLConfigurationResource}
import org.openrepose.core.container.config.ContainerConfiguration
import org.openrepose.core.systemmodel.SystemModel

import scala.reflect.ClassTag

object Marshaller {

  val systemModelXSD = getClass.getResource("/META-INF/schema/system-model/system-model.xsd")
  val containerConfigXSD = getClass.getResource("/META-INF/schema/container/container-configuration.xsd")


  def systemModel(resource: String): SystemModel = {
    configResource[SystemModel](resource, systemModelXSD)
  }

  def containerConfig(resource: String): ContainerConfiguration = {
    configResource[ContainerConfiguration](resource, containerConfigXSD)
  }

  def systemModelString(content: String): SystemModel = {
    configFromString[SystemModel](content, systemModelXSD)
  }

  def configFromString[T: ClassTag](content: String, xsdURL: URL): T = {
    import scala.reflect._
    val ct: ClassTag[T] = classTag[T]
    val parser = JaxbConfigurationParser.getXmlConfigurationParser(
      ct.runtimeClass.asInstanceOf[Class[T]],
      xsdURL,
      this.getClass.getClassLoader)

    val configResource = new ByteArrayConfigurationResource("", content.getBytes(StandardCharsets.UTF_8))

    parser.read(configResource)
  }

  def configResource[T: ClassTag](resource: String, xsdURL: URL): T = {
    import scala.reflect._
    val ct: ClassTag[T] = classTag[T]
    val parser = JaxbConfigurationParser.getXmlConfigurationParser(
      ct.runtimeClass.asInstanceOf[Class[T]],
      xsdURL,
      this.getClass.getClassLoader)

    val configResource = new BufferedURLConfigurationResource(this.getClass.getResource(resource))

    parser.read(configResource)
  }

}
