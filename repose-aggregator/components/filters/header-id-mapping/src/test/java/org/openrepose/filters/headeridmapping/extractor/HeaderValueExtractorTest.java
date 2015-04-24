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
package org.openrepose.filters.headeridmapping.extractor;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.regex.ExtractorResult;
import org.openrepose.filters.headeridmapping.config.HttpHeader;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class HeaderValueExtractorTest {

    public static class WhenExtractingIpAddresses {
        private static String USER_HEADER_NAME = "X-RAX-USER";
        private static String GROUP_HEADER_NAME = "X-RAX-GROUPS";
        private static String INVALID_USER_HEADER_NAME = "X-RAX-USERS";
        private static String MULTIPLE_USER_HEADER_NAME = "MULTIPLE USERS";
        private static String MULTIPLE_USERS = "user1,user2,user3";
        private static String MULTIPLE_GROUP_HEADER_NAME = "MULTIPLE GROUPS";
        private static String MULTIPLE_GROUPS = "group1,group2,group3";
        private static String NON_EXISTENT_HEADER = "Some other header";
        private static String USER_HEADER = "singleuser";
        private static String GROUP_HEADER = "singlegroup";
        private static String INVALID_USER = "unknown";
        private static String DEFAULT_USER_VALUE = "blah";
        private static String DEFAULT_QUALITY_VALUE = ";q=0.1";
        private HttpServletRequest request;
        private HeaderValueExtractor extractor;

        @Before
        public void setUp() {
            request = mock(HttpServletRequest.class);
            extractor = new HeaderValueExtractor(request);

            when(request.getHeader(USER_HEADER_NAME)).thenReturn(USER_HEADER);
            when(request.getHeader(GROUP_HEADER_NAME)).thenReturn(GROUP_HEADER);
            when(request.getHeader(INVALID_USER_HEADER_NAME)).thenReturn(INVALID_USER);
            when(request.getHeader(MULTIPLE_USER_HEADER_NAME)).thenReturn(MULTIPLE_USERS);
            when(request.getHeader(MULTIPLE_GROUP_HEADER_NAME)).thenReturn(MULTIPLE_GROUPS);
            when(request.getRemoteAddr()).thenReturn(DEFAULT_USER_VALUE);
        }

        @Test
        public void shouldExtractHeader() {
            String result = extractor.extractHeader(USER_HEADER_NAME);
            assertEquals("Should find value in header", USER_HEADER, result);
        }

        @Test
        public void shouldNotExtractHeader() {
            String result = extractor.extractHeader(NON_EXISTENT_HEADER);
            assertEquals("Should not find value in invalid header", "", result);
        }


        @Test
        public void shouldGetHeaderIpAddress() {
            List<HttpHeader> headers = new ArrayList<HttpHeader>();
            HttpHeader header = new HttpHeader();
            header.setId(USER_HEADER_NAME);
            header.setUserHeader(USER_HEADER_NAME);
            header.setGroupHeader(GROUP_HEADER_NAME);
            headers.add(header);

            ExtractorResult<String> act = extractor.extractUserGroup(headers);

            assertEquals("Should find Header User", USER_HEADER + DEFAULT_QUALITY_VALUE, act.getResult());
            assertEquals("Should find Header Group", GROUP_HEADER + DEFAULT_QUALITY_VALUE, act.getKey());
        }

        @Test
        public void shouldGetFirstUserFromList() {
            final String userExpected = "user1";
            final String groupExpected = "group1";
            List<HttpHeader> headers = new ArrayList<HttpHeader>();
            HttpHeader header = new HttpHeader();
            header.setId(MULTIPLE_USER_HEADER_NAME);
            header.setUserHeader(MULTIPLE_USER_HEADER_NAME);
            header.setGroupHeader(MULTIPLE_GROUP_HEADER_NAME);
            headers.add(header);

            ExtractorResult<String> act = extractor.extractUserGroup(headers);
            assertEquals("Should find Header User", userExpected + DEFAULT_QUALITY_VALUE, act.getResult());
            assertEquals("Should find Header Group", groupExpected + DEFAULT_QUALITY_VALUE, act.getKey());

        }
    }

}
