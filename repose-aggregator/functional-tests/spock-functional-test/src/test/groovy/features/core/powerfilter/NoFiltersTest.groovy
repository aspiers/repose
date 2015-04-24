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
package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

class NoFiltersTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy/noFilters", params)
        repose.start()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }
        if (repose) {
            repose.stop()
        }
    }

    @Unroll("Repose should act as a basic reverse proxy (pass thru) for HTTP method #method")
    def "Repose should act as a basic reverse proxy (pass thru) for HTTP methods"() {
        given:
        String requestBody = "request body"
        String deproxyEndpoint = "http://localhost:${properties.targetPort}"

        when:
        MessageChain mc = deproxy.makeRequest(url: deproxyEndpoint, requestBody: requestBody)

        then:
        mc.getReceivedResponse().getCode() == HttpServletResponse.SC_OK.toString()

        where:
        method << ["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"]
    }
}
