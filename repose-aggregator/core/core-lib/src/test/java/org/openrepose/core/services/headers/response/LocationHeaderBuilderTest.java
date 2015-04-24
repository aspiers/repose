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
package org.openrepose.core.services.headers.response;

import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import java.net.MalformedURLException;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class LocationHeaderBuilderTest {

    public static class WhenRewritingLocationHeaders {
        private LocationHeaderBuilder instance;
        private HttpServletRequest originalRequest;
        private MutableHttpServletResponse response;

        @Before
        public void setUp() {
            instance = new LocationHeaderBuilder();
            originalRequest = mock(HttpServletRequest.class);
            response = mock(MutableHttpServletResponse.class);

            when(originalRequest.getScheme()).thenReturn("http");
        }
        
        @Test
        public void shouldRemoveRootPath() throws MalformedURLException {
            // original request http://myhost.com:8080/test
            when(originalRequest.getServerName()).thenReturn("myhost.com");
            when(originalRequest.getServerPort()).thenReturn(8080);
            when(originalRequest.getContextPath()).thenReturn("");
            
            // destination http://otherhost.com/mocks/test
            final String destUri = "http://otherhost.com/mocks/test";
            final String requestedContext = "";
            final String rootPath = "/mocks";
            
            when(response.getHeader(eq(CommonHttpHeader.LOCATION.name()))).thenReturn("http://myhost.com:8080/mocks/test");
            
            instance.setLocationHeader(originalRequest, response, destUri, requestedContext, rootPath);
            
            final String expected = "http://myhost.com:8080/test";
            verify(response).setHeader(eq(CommonHttpHeader.LOCATION.name()), eq(expected));
            
        }

        @Test
        public void shouldRemoveRootPath2() throws MalformedURLException {
            // original request http://myhost.com:8080/test
            when(originalRequest.getServerName()).thenReturn("myhost.com");
            when(originalRequest.getServerPort()).thenReturn(8080);
            when(originalRequest.getContextPath()).thenReturn("");
            
            // destination http://otherhost.com/mocks/test
            final String destUri = "http://otherhost.com/mocks/test";
            final String requestedContext = "";
            final String rootPath = "/mocks";
            
            when(response.getHeader(eq(CommonHttpHeader.LOCATION.name()))).thenReturn("http://otherhost.com/mocks/test");
            
            instance.setLocationHeader(originalRequest, response, destUri, requestedContext, rootPath);
            
            final String expected = "http://myhost.com:8080/test";
            verify(response).setHeader(eq(CommonHttpHeader.LOCATION.name()), eq(expected));
            
        }

        @Test
        public void shouldRemoveRootPathAndAddVersion() throws MalformedURLException {
            // original request http://myhost.com:8080/v1/test
            when(originalRequest.getServerName()).thenReturn("myhost.com");
            when(originalRequest.getServerPort()).thenReturn(8080);
            when(originalRequest.getContextPath()).thenReturn("");
            
            // destination http://otherhost.com/mocks/test
            final String destUri = "http://otherhost.com/mocks/test";
            final String requestedContext = "v1";
            final String rootPath = "/mocks";
            
            when(response.getHeader(eq(CommonHttpHeader.LOCATION.name()))).thenReturn("http://otherhost.com/mocks/test");
            
            instance.setLocationHeader(originalRequest, response, destUri, requestedContext, rootPath);
            
            final String expected = "http://myhost.com:8080/v1/test";
            verify(response).setHeader(eq(CommonHttpHeader.LOCATION.name()), eq(expected));
            
        }

        @Test
        public void shouldRemoveRootPathWithoutPort80() throws MalformedURLException {
            // original request http://myhost.com/test
            when(originalRequest.getServerName()).thenReturn("myhost.com");
            when(originalRequest.getServerPort()).thenReturn(80);
            when(originalRequest.getContextPath()).thenReturn("");
            
            // destination http://otherhost.com/mocks/test
            final String destUri = "http://otherhost.com/mocks/test";
            final String requestedContext = "";
            final String rootPath = "/mocks";
            
            when(response.getHeader(eq(CommonHttpHeader.LOCATION.name()))).thenReturn("http://myhost.com/mocks/test");
            
            instance.setLocationHeader(originalRequest, response, destUri, requestedContext, rootPath);
            
            final String expected = "http://myhost.com/test";
            verify(response).setHeader(eq(CommonHttpHeader.LOCATION.name()), eq(expected));
            
        }

        @Test
        public void shouldRemoveRootPathWithoutPort80_2() throws MalformedURLException {
            // original request http://myhost.com/test
            when(originalRequest.getServerName()).thenReturn("myhost.com");
            when(originalRequest.getServerPort()).thenReturn(80);
            when(originalRequest.getContextPath()).thenReturn("");
            
            // destination http://otherhost.com/mocks/test
            final String destUri = "http://otherhost.com/mocks/test";
            final String requestedContext = "";
            final String rootPath = "/mocks";
            
            when(response.getHeader(eq(CommonHttpHeader.LOCATION.name()))).thenReturn("http://otherhost.com/mocks/test");
            
            instance.setLocationHeader(originalRequest, response, destUri, requestedContext, rootPath);
            
            final String expected = "http://myhost.com/test";
            verify(response).setHeader(eq(CommonHttpHeader.LOCATION.name()), eq(expected));
            
        }
		
        @Test
        public void shouldKeepQueryPart() throws MalformedURLException {
            // original request http://myhost.com/test?param=value
            when(originalRequest.getServerName()).thenReturn("myhost.com");
            when(originalRequest.getServerPort()).thenReturn(80);
            when(originalRequest.getContextPath()).thenReturn("");
            
            // destination http://otherhost.com/mocks/test?param=value
            final String destUri = "http://otherhost.com/mocks/test?param=value";
            final String requestedContext = "";
            final String rootPath = "/mocks";
            
            when(response.getHeader(eq(CommonHttpHeader.LOCATION.name()))).thenReturn("http://otherhost.com/mocks/test?param=value");
            
            instance.setLocationHeader(originalRequest, response, destUri, requestedContext, rootPath);
            
            final String expected = "http://myhost.com/test?param=value";
            verify(response).setHeader(eq(CommonHttpHeader.LOCATION.name()), eq(expected));
            
        }
        
        
    }
}
