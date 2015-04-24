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
package org.openrepose.commons.utils.http.header

import org.junit.Test
import org.openrepose.commons.utils.http.ExtendedHttpHeader
import org.openrepose.commons.utils.http.OpenStackServiceHeader
import org.openrepose.commons.utils.http.PowerApiHeader

class SplittableHeaderUtilTest {

    @Test
    void testSplitable() {

        SplittableHeaderUtil splitable = new SplittableHeaderUtil();

        assert splitable.isSplitable("accept")
        assert splitable.isSplitable("ACCEPT")
        assert splitable.isSplitable("Accept")

    }

    @Test
    void testUnSplitable() {

        SplittableHeaderUtil splitable = new SplittableHeaderUtil();

        assert !splitable.isSplitable("unsplitable")
        assert !splitable.isSplitable("user-agent")
    }

    @Test
    void testLargSplitableList() {

        SplittableHeaderUtil splitable = new SplittableHeaderUtil(PowerApiHeader.values())

        assert splitable.isSplitable("x-pp-user")

    }

    @Test
    void testSplitableList() {

        SplittableHeaderUtil splitable = new SplittableHeaderUtil(PowerApiHeader.USER)

        assert splitable.isSplitable("x-pp-user")

    }

    @Test
    void testLargerSplitableList() {

        SplittableHeaderUtil splitable = new SplittableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
                ExtendedHttpHeader.values())

        assert splitable.isSplitable("x-pp-user")
        assert splitable.isSplitable("X-Tenant-Name")
        assert splitable.isSplitable("x-tTl")
        assert !splitable.isSplitable("some-other-header")


    }

    @Test
    public void testWWWAuthenticateNotSplit() throws Exception {
        SplittableHeaderUtil splitable = new SplittableHeaderUtil()

        assert !splitable.isSplitable("www-authenticate")
    }
}
