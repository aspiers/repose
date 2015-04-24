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
package org.openrepose.filters.versioning.domain;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;
import org.openrepose.filters.versioning.util.http.HttpRequestInfo;
import org.openrepose.filters.versioning.util.http.HttpRequestInfoImpl;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class VersionedRequestTest {
    @Ignore
    public static class TestParent {

        protected List<MediaType> mediaRangeList;
        protected ServiceVersionMapping mapping;

        @Before
        public void standUp() {
            mediaRangeList = new LinkedList<MediaType>();
            mediaRangeList.add(new MediaType("", MimeType.UNKNOWN, -1));

            mapping = new ServiceVersionMapping();
            mapping.setId("v1.0");
        }
    }

    public static class WhenIdentifyingVersionsInRequestUris extends TestParent {

        @Test
        public void shouldIdentifyVersion() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/resource", "http://localhost/v1.0/resource", "localhost", "http");
            final VersionedRequest versionedRequest = new VersionedRequest(requestInfo, mapping);

            assertTrue(versionedRequest.requestBelongsToVersionMapping());
        }

        @Test
        public void shouldIdentifyVersionWithTrailingSlash() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/resource/", "http://localhost/v1.0/resource/", "localhost", "http");
            final VersionedRequest versionedRequest = new VersionedRequest(requestInfo, mapping);

            assertTrue(versionedRequest.requestBelongsToVersionMapping());
        }

        @Test
        public void shouldNotMatchPartialVersionMatches() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/v1.01/resource/", "http://localhost/v1.01/resource/", "localhost", "http");
            final VersionedRequest versionedRequest = new VersionedRequest(requestInfo, mapping);

            assertFalse(versionedRequest.requestBelongsToVersionMapping());
        }
    }

    public static class WhenGeneratingInternalMappings extends TestParent {

        @Test(expected = IllegalArgumentException.class)
        public void shouldNotAcceptUriWithoutRoot() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "a/requested/resource", "http://localhost/a/requested/resource", "localhost", "http");

            new VersionedRequest(requestInfo, mapping).asInternalURI();
        }

        @Test
        public void shouldHandleFuzzedRequests() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/v1.0a/requested/resource", "http://localhost/v1.0a/requested/resource", "localhost", "http");

            final String expected = "/v1.0a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping).asInternalURI());
        }

        @Test
        public void shouldHandleNonVersionedRequests() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/a/requested/resource", "http://localhost/a/requested/resource", "localhost", "http");

            final String expected = "/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping).asInternalURI());
        }

        @Test
        public void shouldHandleVersionedRequestsWithContextRoot() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/context/v1.0/a/requested/resource", "http://localhost/context/v1.0/a/requested/resource", "localhost", "http");

            final String expected = "/context/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping).asInternalURI());
        }

        @Test
        public void shouldNotRewriteVersionedUri() {
            final String expected = "/_v1.0/a/requested/resource";
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, expected, "http://localhost/v1.0/a/requested/resource", "localhost", "http");

            final VersionedRequest request = new VersionedRequest(requestInfo, mapping);

            assertEquals("Formatting internal URI must match " + expected, expected, request.asInternalURI());
        }

        @Test
        public void shouldHandleVersionedRequests() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/a/requested/resource", "http://localhost/v1.0/a/requested/resource", "localhost", "http");

            final String expected = "/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping).asInternalURI());
        }

        @Test
        public void shouldBuildAccurateURLs() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/a/requested/resource", "http://localhost/a/requested/resource", "localhost", "http");

            final String expected = "http://localhost/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping).asInternalURL());
        }
    }

    public static class WhenGeneratingExternalMappings extends TestParent {

        @Test
        public void shouldHandleExternalRequestsWithContextRoot() {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/a/requested/resource", "http://localhost/v1.0/a/requested/resource", "localhost", "http");

            final String expected = "http://localhost/v1.0/a/requested/resource";

            assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(requestInfo, mapping).asExternalURL());
        }
    }

    public static class WhenTestingExternalMappings extends TestParent {

        @Test
        public void shouldMatch() {
            final HttpRequestInfo versionOne = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/some/resource", "http://localhost/v1.0", "localhost", "http");
            final HttpRequestInfo versionOneWithResource = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/some/resource", "http://localhost/v1.0/some/resource", "localhost", "http");
            final HttpRequestInfo versionTwo = new HttpRequestInfoImpl(mediaRangeList, "/v2.0/some/resource", "http://localhost/v2.0/some/resource", "localhost", "http");

            assertTrue(new VersionedRequest(versionOne, mapping).requestBelongsToVersionMapping());
            assertTrue(new VersionedRequest(versionOneWithResource, mapping).requestBelongsToVersionMapping());
            assertFalse(new VersionedRequest(versionTwo, mapping).requestBelongsToVersionMapping());
        }

        @Test
        public void shouldIdentifyOwningVersions() {
            final HttpRequestInfo versionOne = new HttpRequestInfoImpl(mediaRangeList, "/v1.0/some/resource", "http://localhost/v1.0/some/resource", "localhost", "http");
            final HttpRequestInfo versionTwo = new HttpRequestInfoImpl(mediaRangeList, "/v2.0/some/resource", "http://localhost/v2.0/some/resource", "localhost", "http");

            assertTrue(new VersionedRequest(versionOne, mapping).requestBelongsToVersionMapping());
            assertFalse(new VersionedRequest(versionTwo, mapping).requestBelongsToVersionMapping());
        }
    }
}
