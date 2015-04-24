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
package org.openrepose.valve

import org.junit.runner.RunWith
import org.openrepose.core.container.config.SslConfiguration
import org.openrepose.core.spring.{CoreSpringProvider, ReposeSpringProperties}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeJettyServerTest extends FunSpec with Matchers {

  CoreSpringProvider.getInstance().initializeCoreContext("/config/root", false);

  val httpPort = 10234
  val httpsPort = 10235

  val sslConfig = {
    val s = new SslConfiguration()
    s.setKeyPassword("password")
    s.setKeystoreFilename("some.file")
    s.setKeystorePassword("lolpassword")

    s
  }

  it("can create a jetty server listening on an HTTP port") {
    val repose = new ReposeJettyServer(
      "cluster",
      "node",
      Some(httpPort),
      None,
      None
    )

    //Cannot verify too much, really can just prove that I have one connector
    repose.server.getConnectors.size shouldBe 1
  }
  it("can create a jetty server listening on an HTTPS port") {
    val repose = new ReposeJettyServer(
      "cluster",
      "node",
      None,
      Some(httpPort),
      Some(sslConfig)
    )

    //Cannot verify too much, really can just prove that I have one connector
    repose.server.getConnectors.size shouldBe 1
  }
  it("can create a jetty server listening on both an HTTP port and an HTTPS port") {
    val repose = new ReposeJettyServer(
      "cluster",
      "node",
      Some(httpPort),
      Some(httpsPort),
      Some(sslConfig)
    )

    //Cannot verify too much, really can just prove that I have one connector
    repose.server.getConnectors.size shouldBe 2

  }

  it("raises an exception when an HTTPS port is specified, but no ssl config is provided") {
    intercept[ServerInitializationException] {
      new ReposeJettyServer(
        "cluster",
        "node",
        None,
        Some(httpPort),
        None
      )
    }
  }
  it("raises an exception when neither HTTP nor HTTPS port are specified") {
    intercept[ServerInitializationException] {
      new ReposeJettyServer(
        "cluster",
        "node",
        None,
        None,
        None
      )
    }
  }

  it("Can terminate a server, shutting down the node's entire context") {
    val server = new ReposeJettyServer(
      "cluster",
      "node",
      Some(httpPort),
      None,
      None
    )

    server.start()
    server.nodeContext.isActive shouldBe true
    server.nodeContext.isRunning shouldBe true

    server.shutdown()
    server.nodeContext.isActive shouldBe false
    server.nodeContext.isRunning shouldBe false

    server.appContext.isActive shouldBe false
    server.appContext.isRunning shouldBe false

  }
  it("can be restarted, terminating and restarting everything") {
    val server = new ReposeJettyServer(
      "cluster",
      "node",
      Some(httpPort),
      None,
      None
    )

    server.start()
    server.appContext.isActive shouldBe true
    server.appContext.isRunning shouldBe true
    server.server.isRunning shouldBe true

    val server2 = server.restart()

    server2.appContext.isActive shouldBe false
    //Cannot check to see if it's running, because it flips out
    server2.server.isRunning shouldBe false

    server2.start()

    server2.server.isRunning shouldBe true
    server2.appContext.isActive shouldBe true
    server2.appContext.isRunning shouldBe true

    //Clean up this server
    server2.shutdown()
  }

  it("Fails when attempting to start a shutdown server") {
    val server = new ReposeJettyServer(
      "cluster",
      "node",
      Some(httpPort),
      None,
      None
    )
    println(s"app context active: ${server.appContext.isActive}")

    server.start()
    server.shutdown()

    println(s"app context active: ${server.appContext.isActive}")

    //TODO: handle this!
    intercept[Exception] {
      server.start()
    }
  }

  it("has the spring properties we need at this stage") {
    val server = new ReposeJettyServer(
      "cluster",
      "le_node_id",
      Some(8080),
      None,
      None
    )
    import ReposeSpringProperties.NODE._
    import ReposeSpringProperties.CORE._


    val expectedProperties = Map(
      CLUSTER_ID -> "cluster",
      NODE_ID -> "le_node_id",
      CONFIG_ROOT -> "/config/root",
      INSECURE -> "false" //Spring puts this into a string for us
    ).map { case (k, v) => ReposeSpringProperties.stripSpringValueStupidity(k) -> v}

    expectedProperties.foreach { case (k, v) =>
      server.appContext.getEnvironment.getProperty(k) shouldBe v
    }

  }

}
