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
package features.filters.identityv3

import framework.ReposeValveTest
import framework.mocks.MockIdentityV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 9/29/14.
 * test option when send-all-project-ids set to false
 */
class MultiProjectIdsHeadersFalseTest extends ReposeValveTest{
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/multiprojectids/sendallprojectidsfalse", params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def setup(){
        fakeIdentityV3Service.resetHandlers()
    }

    @Unroll ("#defaultProject, #secondProject, request project #reqProject")
    def "When user have multi-projects will retrieve all projects to headers" () {
        given:
        fakeIdentityV3Service.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1)
            client_projectid = defaultProject
            client_projectid2 = secondProject
        }

        when: "User passes a request through repose with $reqProject"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqProject",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == serviceRespCode

        if (serviceRespCode != "200")
            assert mc.handlings.size() == 0
        else {
            assert mc.handlings.size() == 1
            assert mc.handlings[0].request.headers.findAll("x-project-id").size() == numberProjects
            assert mc.handlings[0].request.headers.findAll("x-project-id").contains(reqProject)
        }

        where:
        defaultProject  | secondProject   | reqProject      | clientToken       | serviceRespCode   | numberProjects
        "123456"        | "test-project"  | "123456"        |UUID.randomUUID()  | "200"             | 1
        "test-project"  | "12345"         | "12345"         |UUID.randomUUID()  | "200"             | 1
        "123456"        | "123456"        | "test-proj-id"  |UUID.randomUUID()  | "401"             | 0
        "123456"        | "test-project"  | "openstack"     |UUID.randomUUID()  | "401"             | 0
        "123456"        | "test-project"  | ""              |UUID.randomUUID()  | "401"             | 0
    }
}
