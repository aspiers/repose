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
package org.openrepose.core.services.context.impl

import com.google.common.base.Optional
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.mockito.ArgumentCaptor
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.SystemModelInterrogator
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httpclient.HttpClientService
import org.openrepose.core.systemmodel.Node
import org.openrepose.core.systemmodel.NodeList
import org.openrepose.nodeservice.httpcomponent.RequestProxyServiceImpl
import org.openrepose.core.systemmodel.ReposeCluster
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy
import org.openrepose.core.services.healthcheck.Severity
import spock.lang.Shared
import spock.lang.Specification

import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

class RequestProxyServiceHealthCheckTest extends Specification {
    @Shared
    def RequestProxyServiceImpl requestProxyService

    @Shared
    def SystemModelInterrogator systemModelInterrogator

    @Shared
    def ConfigurationService configurationService

    @Shared
    def HealthCheckService healthCheckService

    @Shared
    def HealthCheckServiceProxy healthCheckServiceProxy

    def setup() {
        systemModelInterrogator = mock(SystemModelInterrogator.class)
        configurationService = mock(ConfigurationService.class)
        healthCheckService = mock(HealthCheckService.class)
        healthCheckServiceProxy = mock(HealthCheckServiceProxy)

        when(healthCheckService.register()).thenReturn(healthCheckServiceProxy)

        this.requestProxyService = new RequestProxyServiceImpl(configurationService, healthCheckService, mock(HttpClientService.class), "cluster", "node")

    }

    def "if localhost can find self in system model on update, should resolve outstanding issues with health check service"() {
        given:
        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)
        def localCluster = mock(ReposeCluster.class)

        when(localCluster.isRewriteHostHeader()).thenReturn(false)
        when(systemModelInterrogator.getLocalCluster(any(SystemModel.class))).thenReturn(Optional.of(localCluster))
        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        requestProxyService.init()

        listenerObject = listenerCaptor.getValue()

        def config = new SystemModel()
        config.reposeCluster = new LinkedList<ReposeCluster>()
        config.reposeCluster.add(new ReposeCluster())
        config.reposeCluster.first().id = "cluster"
        config.reposeCluster.first().nodes = new NodeList()
        config.reposeCluster.first().nodes.node = new LinkedList<Node>()
        config.reposeCluster.first().nodes.node.add(new Node())
        config.reposeCluster.first().nodes.node.first().id = "node"


        when:
        listenerObject.configurationUpdated(config)

        then:
        listenerObject.isInitialized()
        verify(healthCheckServiceProxy).resolveIssue(eq(RequestProxyServiceImpl.SYSTEM_MODEL_CONFIG_HEALTH_REPORT))
    }

    def "if localhost cannot find self in system model on update, should log error and report to health check service"() {
        given:
        def listenerObject
        def listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class)
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false)
        ListAppender app = ((ListAppender) (ctx.getConfiguration().getAppender("List0"))).clear()

        when(systemModelInterrogator.getLocalCluster(any(SystemModel.class))).thenReturn(Optional.absent())
        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class))

        requestProxyService.init()

        listenerObject = listenerCaptor.getValue()
        def config = new SystemModel()
        config.reposeCluster = new LinkedList<ReposeCluster>()
        config.reposeCluster.add(new ReposeCluster())
        config.reposeCluster.first().id = "cluster"
        config.reposeCluster.first().nodes = new NodeList()
        config.reposeCluster.first().nodes.node = new LinkedList<Node>()
        config.reposeCluster.first().nodes.node.add(new Node())
        config.reposeCluster.first().nodes.node.first().id = "nope"


        when:
        listenerObject.configurationUpdated(config)

        then:
        !listenerObject.isInitialized()
        app.getEvents().find {
            it.getMessage().getFormattedMessage().contains("Unable to identify the local host (cluster:cluster, node:node) in the system model")
        }
        verify(healthCheckServiceProxy).reportIssue(eq(RequestProxyServiceImpl.SYSTEM_MODEL_CONFIG_HEALTH_REPORT), any(String.class),
                any(Severity))
    }
}
