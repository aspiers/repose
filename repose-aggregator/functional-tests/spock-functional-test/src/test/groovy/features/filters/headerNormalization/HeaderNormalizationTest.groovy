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
package features.filters.headerNormalization

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * User: dimi5963
 * Date: 9/9/13
 * Time: 10:55 AM
 */
class HeaderNormalizationTest extends ReposeValveTest {

    static Map params = [:]

    def headers = [
            'user1'          : 'usertest1',
            'X-Auth-Token'   : '358484212:99493',
            'X-First-Filter' : 'firstValue',
            'X-SeCoND-Filter': 'secondValue',
            'X-third-filter' : 'thirdValue',
            'X-last-Filter'  : 'lastValue',
            'X-User-Token'   : 'something'
    ]

    def setupSpec() {
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/headerNormalization", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "When Filtering Based on URI and Method"() {
        when:
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method : 'GET',
                                url    : reposeEndpoint + "/v1/usertest1/servers/something",
                                headers: headers
                        ])

        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-auth-token") == '358484212:99493'
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-first-filter") == 'firstValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-second-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-third-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains("1.1 localhost:${properties.reposePort} (Repose/")
        mc.receivedResponse.code == '200'
    }

    def "When Filtering Based on URI"() {
        when:
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method : 'POST',
                                url    : reposeEndpoint + "/v1/usertest1/servers/something",
                                headers: headers
                        ])

        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-auth-token") == '358484212:99493'
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-second-filter") == 'secondValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-first-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-third-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains("1.1 localhost:${properties.reposePort} (Repose/")
        mc.receivedResponse.code == '200'

    }

    def "When Filtering Based on Method"() {
        when:
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method : 'POST',
                                url    : reposeEndpoint + "/v1/usertest1/resources/something",
                                headers: headers
                        ])
        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-auth-token") == '358484212:99493'
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-third-filter") == 'thirdValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-second-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-first-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains("1.1 localhost:${properties.reposePort} (Repose/")
        mc.receivedResponse.code == '200'
    }

    def "When Filtering using catch all"() {
        when:
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method : 'GET',
                                url    : reposeEndpoint + "/v1/usertest1/resources/something",
                                headers: headers
                        ])
        then:
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
        mc.handlings[0].request.headers.findAll("x-auth-token") == []
        mc.handlings[0].request.headers.getFirstValue("x-user-token") == 'something'
        mc.handlings[0].request.headers.getFirstValue("user1") == 'usertest1'
        mc.handlings[0].request.headers.findAll("x-last-filter") == []
        mc.handlings[0].request.headers.getFirstValue("x-second-filter") == 'secondValue'
        mc.handlings[0].request.headers.getFirstValue("x-third-filter") == 'thirdValue'
        mc.handlings[0].request.headers.getFirstValue("x-first-filter") == 'firstValue'
        mc.handlings[0].request.headers.getFirstValue("via").contains("1.1 localhost:${properties.reposePort} (Repose/")
        mc.receivedResponse.code == '200'
    }

    def "Should split request headers according to rfc by default"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
                [
                        "user-agent": userAgentValue,
                        "x-pp-user" : "usertest1, usertest2, usertest3",
                        "accept"    : "application/xml;q=1 , application/json;q=0.5"
                ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: handler)
        def handling = mc.getHandlings()[0]

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }

    @Unroll("Requests - headers: #headerName with \"#headerValue\" keep its case")
    def "Requests - headers should keep its case in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains(headerName)
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue


        where:
        headerName         | headerValue
        "Accept"           | "text/plain"
        "ACCEPT"           | "text/PLAIN"
        "accept"           | "TEXT/plain;q=0.2"
        "aCCept"           | "text/plain"
        "CONTENT-Encoding" | "identity"
        "Content-ENCODING" | "identity"
        //"content-encoding" | "idENtItY"
        //"Content-Encoding" | "IDENTITY"
    }

    @Unroll("Responses - headers: #headerName with \"#headerValue\" keep its case")
    def "Responses - header keep its case in responses"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: { new Response(200, null, headers) })

        then: "the response should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.contains(headerName)
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue


        where:
        headerName     | headerValue
        "x-auth-token" | "123445"
        "X-AUTH-TOKEN" | "239853"
        "x-AUTH-token" | "slDSFslk&D"
        "x-auth-TOKEN" | "sl4hsdlg"
        "CONTENT-Type" | "application/json"
        "Content-TYPE" | "application/json"
        //"content-type" | "application/xMl"
        //"Content-Type" | "APPLICATION/xml"
    }



    def "Should not split request headers when configured as such with merge-header filter"() {
        given: "configurged to to split headers using the merge-header filter"

        repose.configurationProvider.applyConfigs("features/filters/headerNormalization/withmergeheaderfilter", params)
        sleep 15000


        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
                [
                        "user-agent": userAgentValue,
                        "x-pp-user" : "usertest1, usertest2, usertest3",
                        "accept"    : "application/xml;q=1 , application/json;q=0.5"
                ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 1
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 1
    }

    def "Should merge response headers when configured as such with merge-header filter"() {
        given: "Origin service returns headers "
        def respHeaders = ["aurl": "http://somehost.com/blah", "aurl": "http://somehost.com/bloo", "type": "application/xml", "type": "application/json"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: handler)
        def handling = mc.getHandlings()[0]

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("aurl").size() == 1
        mc.receivedResponse.headers['aurl'].contains("http://somehost.com/bloo")
        mc.receivedResponse.headers.findAll("via").size() == 1
    }

}
