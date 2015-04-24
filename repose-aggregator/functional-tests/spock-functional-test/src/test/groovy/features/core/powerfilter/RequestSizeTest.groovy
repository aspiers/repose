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
import org.apache.commons.lang.RandomStringUtils
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll
import org.rackspace.deproxy.Header

/**
 * Setup: the configuration for this test has a container.cfg.xml with a content-body-read-limit="32000"
 */
class RequestSizeTest extends ReposeValveTest {

    String charset = (('A'..'Z') + ('0'..'9')).join()

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/requestsize", params)
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    @Unroll("request with header size of #headerSize should respond with 413")
    def "max header size allowed is not influenced by content-body-read-limit"() {

        given: "I have headers that exceed the header size limit"
        def header1 = RandomStringUtils.random(headerSize, charset)

        when: "I send a request to REPOSE with my headers"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: [headerName: header1])

        then: "I get a response of 413"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0

        where:
        headerName | headerSize
        "Header1"  | 32000
        "Header1"  | 16000
        "Header1"  | 8068
    }

    @Unroll("request with header size of #headerSize should respond with 200")
    def "headers within jetty default size limit are allowed through"() {

        when: "I get a request to verify the header sizes coming through by default"
        //Get the headers that go through normally, so we can do maths to figure out the size limit...
        int defaultHeadersSize = 0
        MessageChain fmc = deproxy.makeRequest(url: reposeEndpoint)
        for(Header hdr : fmc.sentRequest.headers._headers){
            defaultHeadersSize += hdr.value.length()
        }
        int largeHeaderSize = headerSize - defaultHeadersSize
        def header1 = RandomStringUtils.random(largeHeaderSize, charset)

        then: "The first request got a 200"
        fmc.receivedResponse.code == "200"
        fmc.handlings.size() == 1

        when: "I send a second request to REPOSE with my headers"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: [headerName: header1])

        then: "I get a response of 200"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        where:
        headerName | headerSize
        "Header1"  | 8067
        "Header1"  | 5000
        "Header1"  | 4500
        "Header1"  | 4000
    }


}
