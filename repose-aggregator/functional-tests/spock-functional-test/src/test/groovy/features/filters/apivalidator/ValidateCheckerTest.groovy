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
package features.filters.apivalidator

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

import java.util.concurrent.TimeoutException

@Category(Slow.class)
class ValidateCheckerTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    @Unroll
    def "don't expect timeout when checker is #configPath"() {
        given:
        repose.stop()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/pcchecker/$configPath", params)
        repose.start()

        when:
        repose.waitForNon500FromUrl(reposeEndpoint, 30)

        then:
        notThrown(TimeoutException.class)

        where:
        configPath << ["validwithoutvalidation", "validwithvalidation"]
    }

    @Unroll
    def "expect request timeout when check is #configPath"() {
        given:
        repose.stop()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/pcchecker/$configPath", params)
        repose.start()

        when:
        repose.waitForNon500FromUrl(reposeEndpoint, 30)

        then:
        thrown(TimeoutException.class) // Thrown when processing a request, not at initialization
        reposeLogSearch.searchByString("java.util.NoSuchElementException: key not found: SE9001").size() >= 1
        reposeLogSearch.searchByString("/xsl/meta-check.xsl; lineNumber: 37; cvc-id.1: " +
                "There is no ID/IDREF binding for IDREF 'SE9001'").size() == 0

        where:
        configPath << ["invalidwithoutvalidation"]
    }

    @Unroll
    def "expect initialization timeout when checker is #configPath"() {
        given:
        repose.stop()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/pcchecker/$configPath", params)
        repose.start()

        when:
        repose.waitForNon500FromUrl(reposeEndpoint, 30)

        then:
        thrown(TimeoutException.class) // Thrown at initialization, and when processing requests
        reposeLogSearch.searchByString("/xsl/meta-check.xsl; lineNumber: 37; cvc-id.1: " +
                "There is no ID/IDREF binding for IDREF 'SE9001'").size() >= 1

        where:
        configPath << ["invalidwithvalidation"]
    }
}
