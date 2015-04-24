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
package org.openrepose.filters.uriidentity;



import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterDirector;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class UriIdentityHandlerTest {

    public static class WhenHandlingRequests {

        private List<Pattern> patterns;
        private static String GROUP = "DEFAULT_GROUP";

        private static Double QUALITY = 0.5;
        private static String QUALITY_VALUE = ";q=0.5";
        private static String URI1 = "/someuri/1234/morestuff";
        private static String REGEX1 = ".*/[^\\d]*/(\\d*)/.*";
        private static String USER1 = "1234";
        private static String URI2 = "/someuri/abc/someuser";
        private static String REGEX2 = ".*/[^\\d]*/abc/(.*)";
        private static String USER2 = "someuser";
        private static String URIFAIL = "/nouserinformation";
        private HttpServletRequest request;
        private ReadableHttpServletResponse response;
        private UriIdentityHandler handler;

        @Before
        public void setUp() {

            patterns = new ArrayList<Pattern>();
            patterns.add(Pattern.compile(REGEX1));
            patterns.add(Pattern.compile(REGEX2));

            handler = new UriIdentityHandler(patterns, GROUP, QUALITY);
            request = mock(HttpServletRequest.class);
            response = mock(ReadableHttpServletResponse.class);

        }

        @Test
        public void shouldSetTheUserHeaderToTheRegexResult() {
            when(request.getRequestURI()).thenReturn(URI1);

            FilterDirector result = handler.handleRequest(request, response);

            Set<String> values = result.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));
            assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values == null || values.isEmpty());

            String userName = values.iterator().next();

            assertEquals("Should find user name in header", USER1 + QUALITY_VALUE, userName);
        }

        @Test
        public void shouldSetTheUserHeaderToThe2ndRegexResult() {
            when(request.getRequestURI()).thenReturn(URI2);

            FilterDirector result = handler.handleRequest(request, response);

            Set<String> values = result.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));
            assertFalse("Should have " + PowerApiHeader.USER.toString() + " header set.", values == null || values.isEmpty());

            String userName = values.iterator().next();

            assertEquals("Should find user name in header", USER2 + QUALITY_VALUE, userName);
        }

        @Test
        public void shouldNotHaveUserHeader() {
            when(request.getRequestURI()).thenReturn(URIFAIL);

            FilterDirector result = handler.handleRequest(request, response);

            Set<String> values = result.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString()));
            assertTrue("Should not have " + PowerApiHeader.USER.toString() + " header set.", values == null || values.isEmpty());

        }
    }
}