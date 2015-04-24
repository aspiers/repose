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
package org.openrepose.commons.utils.http

import org.junit.Test
/**
 * Some groovy tests as an experiment.
 */

class EndpointsHeaderTest {

    @Test
    void matchesShouldReturnTrueWhenComparing2StringsAndShouldIgnoreCase() {

        def eph = EndpointsHeader.X_CATALOG

        def headers = ["x-CaTaLoG", "x-CATALOG", "X-catalog"]

        for (String h : headers) {
            def matches = eph.matches(h)

            assert matches == true
        }

    }

    @Test
    void matchesShouldReturnFalseWhenStringDoesNotMatch() {

        def eph = EndpointsHeader.X_CATALOG
        def headers = ["asdf", "x-CTALOG", "X-catalogd", "xx-catalog"]

        for (String h : headers) {
            def matches = eph.matches(h)

            assert matches == false
        }

    }
}
