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
package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/*
 * Api validator tests ported over from and JMeter
 */

class ApiValidatorTest extends ReposeValveTest {

    private final String baseGroupPath = "/wadl/group1"
    private final String baseDefaultPath = "/wadl/default"

    private final Map<String, String> defaultHeaders = [
            "Accept"         : "application/xml",
            "Host"           : "localhost",
            "Accept-Encoding": "identity",
            "User-Agent"     : "gdeproxy"
    ]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/jmeter", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("Happy path: when no role passed, should get default wadl - #request")
    def "Happy path: when no role passed, should get default wadl"() {
        setup: "declare messageChain to be of type MessageChain"
        MessageChain messageChain
        def customHandler = { return new Response(200, "OK", [], reqBody) }

        when:
        "When Requesting " + method + " " + request
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                request, method: method, headers: defaultHeaders,
                requestBody: reqBody, defaultHandler: customHandler,
                addDefaultHeaders: false
        )

        then:
        "result should be " + responseCode
        messageChain.receivedResponse.code.equals(responseCode)

//        messageChain.receivedResponse.body.contains("XML Not Authorized... Syntax highlighting is magical.")

        where:
        responseCode | request                                               | method | reqBody
        "200"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"  | "GET"  | ""
        "404"        | "/resource1x/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa" | "GET"  | ""
        "405"        | "/resource1/id"                                       | "POST" | ""
        "415"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"  | "PUT"  | "some data"

    }

    def "Happy path: when Group Passed, Should Get Role Specific WADL"() {
        setup: "declare messageChain to be of type MessageChain, additional headers"
        MessageChain messageChain
        Map<String, String> headers = ["X-Roles": "group1", "Content-Type": "application/xml"]

        when: "When Requesting resource with x-roles"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", method: "GET", headers: defaultHeaders + headers)

        then: "should return resource"
        messageChain.receivedResponse.code.equals("200")

        when: "When Requesting invalid resource with x-roles"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1x/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", method: "GET", headers: defaultHeaders + headers)

        then: "should return not found"
        messageChain.receivedResponse.code.equals("404")
        messageChain.receivedResponse.body.contains("XML Not Authorized... Syntax highlighting is magical.")

        when: "When using invalid method with x-roles"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1/id", method: "POST", headers: defaultHeaders + headers)

        then: "should return not found"
        messageChain.receivedResponse.code.equals("405")
        messageChain.receivedResponse.body.contains("XML Not Authorized... Syntax highlighting is magical.")

        when: "When using valid media type with x-roles"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", method: "POST", headers: defaultHeaders + headers,
                requestBody: "<c xmlns='http://test.openrespose/test/v1.1'><test>some data</test></c>")

        then: "should return OK"
        messageChain.receivedResponse.code.equals("200")
        // Origin service handler does not return a body
        // messageChain.receivedResponse.body.contains("some data")
    }

    def "Happy path: when Ignore XSD Extension enabled"() {
        setup: "declare messageChain to be of type MessageChain, additional headers"
        MessageChain messageChain
        Map<String, String> headers = ["X-Roles": "default", "Content-Type": "application/xml"]

        when: "When Requesting with valid content"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource2/unvalidated/echobody", method: "PUT", headers: defaultHeaders + headers,
                requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></nodeList></c>")

        then: "should return resource"
        messageChain.receivedResponse.code.equals("200")
        // Origin service handler does not return a body
        // messageChain.receivedResponse.body.contains("<node hostname=\"localhost\" http-port=\"8088\" id=\"proxy-n01\"/>")

        when: "When Requesting with invalid content and Ignore XSD enabled"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource2/unvalidated/echobody", method: "PUT", headers: defaultHeaders + headers,
                requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node2 id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></nodeList></c>")

        then: "should return resource"
        messageChain.receivedResponse.code.equals("200")
        // Origin service handler does not return a body
        // messageChain.receivedResponse.body.contains("<node2 hostname=\"localhost\" http-port=\"8088\" id=\"proxy-n01\"/>")
    }

    def "Happy path: When Ignore XSD Extension disabled"() {
        setup: "declare messageChain to be of type MessageChain, additional headers"
        MessageChain messageChain
        Map<String, String> headers = ["X-Roles": "default2", "Content-Type": "application/xml"]

        when: "When Requesting with valid content"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource2/unvalidated/echobody", method: "PUT", headers: defaultHeaders + headers,
                requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></nodeList></c>")

        then: "should return resource"
        messageChain.receivedResponse.code.equals("200")
        // Origin service handler does not return a body
        // messageChain.receivedResponse.body.contains("<node hostname=\"localhost\" http-port=\"8088\" id=\"proxy-n01\"/>")

        when: "When Requesting with invalid content"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource2/unvalidated/echobody", method: "PUT", headers: defaultHeaders + headers,
                requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node2 id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></nodeList></c>")

        then: "should return resource"
        messageChain.receivedResponse.code.equals("400")
        messageChain.receivedResponse.body.contains("One of '{\"http://test.openrespose/test/v1.1\":node}' is expected")

        when: "When Requesting with non well-formed content"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource2/unvalidated/echobody", method: "PUT", headers: defaultHeaders + headers,
                requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></c>")

        then: "should return resource"
        messageChain.receivedResponse.code.equals("400")
        messageChain.receivedResponse.body.contains("The element type \"nodeList\" must be terminated by the matching end-tag")
    }

    def "Happy path: When Passing to resource with required header"() {
        setup: "declare messageChain to be of type MessageChain, additional headers"
        MessageChain messageChain
        Map<String, String> headers = ["x-required-header": "somevalue"]

        when: "When Requesting default resource with no roles and required header"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource1/id/reqheader", method: "GET", headers: defaultHeaders + headers)

        then: "should return resource"
        messageChain.receivedResponse.code.equals("200")
    }

    def "Unhappy path: When Passing to resource without required header"() {
        setup: "declare messageChain to be of type MessageChain"
        MessageChain messageChain

        when: "When Requesting default resource with no roles without required header"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource1/id/reqheader", method: "GET", headers: defaultHeaders)

        then: "should return resource"
        messageChain.receivedResponse.code.equals("400")
        messageChain.receivedResponse.body.contains("Expecting an HTTP header x-required-header")
    }

    def "Should not split request headers according to rfc"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
                [
                        "user-agent": userAgentValue,
                        "x-pp-user" : "usertest1, usertest2, usertest3",
                        "accept"    : "application/xml;q=1 , application/json;q=0.5",
                        "X-Roles"   : "group1"
                ]

        when: "When Requesting resource with x-roles"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", method: "GET", headers: reqHeaders)
        def handling = messageChain.getHandlings()[0]

        then:
        handling.request.getHeaders().findAll("user-agent").size() == 1
        handling.request.headers['user-agent'] == userAgentValue
        handling.request.getHeaders().findAll("x-pp-user").size() == 3
        handling.request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def xmlResp = { request -> return new Response(201, "Created", respHeaders, "") }
        Map<String, String> headers = ["X-Roles": "group1", "Content-Type": "application/xml"]


        when: "client passes a request through repose with headers"
        def MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", method: "GET", headers: headers,
                defaultHandler: xmlResp)

        then:
        messageChain.receivedResponse.headers.findAll("location").size() == 1
        messageChain.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        messageChain.receivedResponse.headers.findAll("via").size() == 1
    }

    @Unroll("With headers: #xppuser-#xppuservalue, #accept-#acceptvalue, #roles-#rolevalue")
    def "Should not toLowerCase headers"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
                [
                        "user-agent": userAgentValue,
                ]
        reqHeaders[xppuser.toString()] = xppuservalue.toString()
        reqHeaders[accept.toString()] = acceptvalue.toString()
        reqHeaders[roles.toString()] = rolevalue.toString()

        when: "When Requesting resource with x-roles"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", method: "GET", headers: reqHeaders)
        def handling = messageChain.getHandlings()[0]

        then:
        handling.request.getHeaders().findAll("user-agent").size() == 1
        handling.request.headers['user-agent'] == userAgentValue
        handling.request.getHeaders().findAll(xppuser).size() == xppuservalue.split(',').size()
        handling.request.getHeaders().findAll(accept).size() == acceptvalue.split(',').size()
        handling.request.headers.contains(xppuser)
        handling.request.headers.findAll(xppuser) == xppuservalue.split(',')
        handling.request.headers.contains(accept)
        handling.request.headers.findAll(accept) == acceptvalue.split(',')
        handling.request.headers.contains(roles)
        handling.request.headers.findAll(roles) == rolevalue.split(',')

        where:
        xppuser     | xppuservalue          | accept   | acceptvalue                        | roles     | rolevalue
        "x-pp-user" | "usertest1,usertest2" | "accept" | "application/xml,application/json" | "x-roles" | "group1"
        "X-pp-user" | "User1,user2"         | "Accept" | "Application/xml,application/JSON" | "X-roles" | "group1,Group2"
        "X-PP-User" | "USER1,user2,User2"   | "ACCEPT" | "APPLICATION/XML"                  | "X-Roles" | "group1,role1"
        "X-PP-USER" | "USERTEST"            | "accEPT" | "application/XML,text/plain"       | "X-ROLES" | "ROLE1,group1,ROLE30"
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
        //"Content-Type" | "application/XML"
    }
}
