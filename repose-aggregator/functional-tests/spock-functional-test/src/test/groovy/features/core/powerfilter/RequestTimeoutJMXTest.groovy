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
package features.core.powerfilter

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response

@Category(Slow.class)
class RequestTimeoutJMXTest extends ReposeValveTest {

    String PREFIX = "\"${jmxHostname}-org.openrepose.core\":type=\"RequestTimeout\",scope=\""

    String NAME_OPENREPOSE_ENDPOINT = "\",name=\"localhost:${properties.targetPort}/root_path\""
    String ALL_ENDPOINTS = "\",name=\"All Endpoints\""

    String TIMEOUT_TO_ORIGIN = PREFIX + "TimeoutToOrigin" + NAME_OPENREPOSE_ENDPOINT
    String ALL_TIMEOUT_TO_ORIGIN = PREFIX + "TimeoutToOrigin" + ALL_ENDPOINTS

    def handlerTimeout = { request -> return new Response(408, 'WIZARD FAIL') }

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/common", params)
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()
        repose.stop()
    }

    def "when responses have timed out, should increment RequestTimeout mbeans for specific endpoint"() {
        given:
        def target = repose.jmx.quickMBeanAttribute(TIMEOUT_TO_ORIGIN, "Count")
        target = (target == null) ? 0 : target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])

        then:
        repose.jmx.getMBeanAttribute(TIMEOUT_TO_ORIGIN, "Count") == (target + 2)
    }


    def "when responses have timed out, should increment RequestTimeout mbeans for all endpoint"() {
        given:
        def target = repose.jmx.quickMBeanAttribute(ALL_TIMEOUT_TO_ORIGIN, "Count")
        target = (target == null) ? 0 : target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])

        then:
        repose.jmx.getMBeanAttribute(ALL_TIMEOUT_TO_ORIGIN, "Count") == (target + 2)
    }

    def "when SOME responses have timed out, should increment RequestTimeout mbeans for specific endpoint only for timeouts"() {
        given:
        def target = repose.jmx.quickMBeanAttribute(ALL_TIMEOUT_TO_ORIGIN, "Count")
        target = (target == null) ? 0 : target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest(url:reposeEndpoint + "/endpoint")
        deproxy.makeRequest(url:reposeEndpoint + "/endpoint")

        then:
        repose.jmx.getMBeanAttribute(ALL_TIMEOUT_TO_ORIGIN, "Count") == (target + 2)
    }
}
