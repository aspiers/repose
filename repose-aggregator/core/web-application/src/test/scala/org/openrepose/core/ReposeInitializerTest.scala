/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
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
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core

import java.util
import javax.servlet.{DispatcherType, FilterRegistration, ServletContext, ServletRegistration}

import org.junit.runner.RunWith
import org.mockito.Mockito.{verify, when}
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.powerfilter.EmptyServlet
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import org.springframework.web.context.ContextLoaderListener
import org.springframework.web.filter.DelegatingFilterProxy

@RunWith(classOf[JUnitRunner])
class ReposeInitializerTest extends FunSpec with Matchers with MockitoSugar{

  {
    //NOTE: have to set up these things since the initializer needs this stuff
    import ReposeSpringProperties._
    System.setProperty(stripSpringValueStupidity(CORE.CONFIG_ROOT), "/config/root")
    System.setProperty(stripSpringValueStupidity(CORE.INSECURE), "false")
    System.setProperty(stripSpringValueStupidity(NODE.CLUSTER_ID), "cluster")
    System.setProperty(stripSpringValueStupidity(NODE.NODE_ID), "node")
  }

  describe("The repose initializer") {
    it("should add the core context to the servlet context") {
      //TODO: I dont know how to get to the context to see whats in it yet....
      val initializer = new ReposeInitializer
      val context: ServletContext = mock[ServletContext]
      when(context.addServlet("emptyServlet", classOf[EmptyServlet])).thenReturn(mock[ServletRegistration.Dynamic])
      when(context.addFilter(org.mockito.Matchers.eq("springDelegatingFilterProxy"), org.mockito.Matchers.any(classOf[DelegatingFilterProxy]))).thenReturn(mock[FilterRegistration.Dynamic])
      initializer.onStartup(context)
      verify(context).addListener(org.mockito.Matchers.any(classOf[ContextLoaderListener]))
    }
    it("should map the empty servlet to root on the servlet context") {
      val initializer: ReposeInitializer = new ReposeInitializer
      val context: ServletContext = mock[ServletContext]
      val servletRegistration: ServletRegistration.Dynamic = mock[ServletRegistration.Dynamic]
      when(context.addServlet("emptyServlet", classOf[EmptyServlet])).thenReturn(servletRegistration)
      when(context.addFilter(org.mockito.Matchers.eq("springDelegatingFilterProxy"), org.mockito.Matchers.any(classOf[DelegatingFilterProxy]))).thenReturn(mock[FilterRegistration.Dynamic])
      initializer.onStartup(context)
      verify(servletRegistration).addMapping("/*")
    }
    it("should map the main filter to root on the servlet context") {
      val initializer = new ReposeInitializer
      val context: ServletContext = mock[ServletContext]
      when(context.addServlet("emptyServlet", classOf[EmptyServlet])).thenReturn(mock[ServletRegistration.Dynamic])
      val filterRegistration: FilterRegistration.Dynamic = mock[FilterRegistration.Dynamic]
      when(context.addFilter(org.mockito.Matchers.eq("springDelegatingFilterProxy"), org.mockito.Matchers.any(classOf[DelegatingFilterProxy]))).thenReturn(filterRegistration)
      initializer.onStartup(context)
      verify(filterRegistration).addMappingForUrlPatterns(util.EnumSet.allOf(classOf[DispatcherType]), false, "/*")
    }
  }
}
