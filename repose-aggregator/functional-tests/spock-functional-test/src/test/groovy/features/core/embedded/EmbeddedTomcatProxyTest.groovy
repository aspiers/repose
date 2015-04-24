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
package features.core.embedded
import org.openrepose.commons.utils.test.mocks.util.MocksUtil
import org.openrepose.commons.utils.test.mocks.util.RequestInfo
import framework.ReposeConfigurationProvider
import framework.ReposeContainerLauncher
import framework.ReposeLauncher
import framework.ReposeLogSearch
import framework.TestProperties
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Specification

class EmbeddedTomcatProxyTest extends Specification {

    static ReposeLauncher repose
    static Deproxy deproxy
    static String tomcatEndpoint

    def setupSpec() {
        def TestProperties properties = new TestProperties()
        ReposeLogSearch log = new ReposeLogSearch(properties.logFile);
        log.cleanLog()
        int originServicePort = properties.targetPort
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)

        int reposePort = properties.reposePort
        tomcatEndpoint = "http://localhost:${reposePort}"

        def configDirectory = properties.getConfigDirectory()
        def configTemplates = properties.getRawConfigDirectory()
        def rootWar = properties.getReposeRootWar()
        def buildDirectory = properties.getReposeHome() + "/.."
        def mocksWar = properties.getMocksWar()
        def mocksPath = MocksUtil.getServletPath(mocksWar)

        ReposeConfigurationProvider config = new ReposeConfigurationProvider(configDirectory, configTemplates)

        def params = properties.getDefaultTemplateParams()
        params += [
                'reposePort': reposePort,
                'targetPort': originServicePort,
                'repose.config.directory': configDirectory,
                'repose.cluster.id': "repose1",
                'repose.node.id': 'node1',
                'appPath':  mocksPath
        ]
        config.cleanConfigDirectory()
        config.applyConfigs("common", params)
        config.applyConfigs("features/filters/ipidentity", params)

        config.applyConfigs("features/core/embedded", params)

        repose = new ReposeContainerLauncher(config, properties.getTomcatJar(), "repose1", "node1", rootWar, reposePort, mocksWar)
        repose.enableDebug()
        repose.clusterId = "repose"
        repose.start()
        repose.waitForNon500FromUrl(tomcatEndpoint, 120)
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop()
    }

    def "Should Pass Requests through repose"() {

        when: "Request is sent through Repose/Tomcat"
        MessageChain mc = deproxy.makeRequest(url: tomcatEndpoint + "/cluster?a=b&c=123", headers: ['passheader': 'value1', 'PassHeAder' : 'value2'])
        RequestInfo info = MocksUtil.xmlStringToRequestInfo(mc.receivedResponse.body.toString())

        then: "Repose Should Forward Response"
        mc.receivedResponse.code == "200"

        and: "Response should contain a body"
        !mc.receivedResponse.body.toString().empty

        and: "Repose should have passed the pass header"
        info.getHeaders().get("passheader").get(0) == "value1"
        info.getHeaders().get("pAssHeader").size() == 2

        and: "Repose should have passed query params"
        info.getQueryParams().get("a").get(0).equals("[b]")
        info.getQueryParams().size() == 2
    }
}
