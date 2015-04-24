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
package org.openrepose.filters.urinormalization.normalizer;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.filters.urinormalization.config.MediaType;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class MediaTypeNormalizerTest {

    public static class WhenNormalizingVariantExtensions {

        private List<MediaType> configuredMediaTypes;
        private MediaTypeNormalizer normalizer;
        
        @Before
        public void standUp() {
            configuredMediaTypes = new LinkedList<MediaType>();

            final MediaType configuredMediaType = new MediaType();
            configuredMediaType.setName("application/xml");
            configuredMediaType.setVariantExtension("xml");
            configuredMediaType.setPreferred(Boolean.TRUE);

            configuredMediaTypes.add(configuredMediaType);
            
            normalizer = new MediaTypeNormalizer(configuredMediaTypes);
        }

        @Test
        public void shouldCorrectlyCaptureVariantExtensions() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri.xml"));
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
            assertEquals("/a/request/uri", director.getRequestUri());
            assertEquals("http://localhost/a/request/uri", director.getRequestUrl().toString());
        }

        @Test
        public void shouldCorrectlyIgnoreQueryParameters() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml?name=name&value=1");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri.xml?name=name&value=1"));
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
            assertEquals("/a/request/uri?name=name&value=1", director.getRequestUri());
            assertEquals("http://localhost/a/request/uri?name=name&value=1", director.getRequestUrl().toString());
        }

        @Test
        public void shouldCorrectlyIgnoreUriFragments() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml#fragment");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri.xml#fragment"));
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
            assertEquals("/a/request/uri#fragment", director.getRequestUri());
            assertEquals("http://localhost/a/request/uri#fragment", director.getRequestUrl().toString());
        }

        @Test
        public void shouldCorrectlyIgnoreUriFragmentsAndQueryParameters() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml?name=name&value=1#fragment");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri.xml?name=name&value=1#fragment"));
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
            assertEquals("/a/request/uri?name=name&value=1#fragment", director.getRequestUri());
            assertEquals("http://localhost/a/request/uri?name=name&value=1#fragment", director.getRequestUrl().toString());
        }

        @Test
        public void shouldCorrectlyCaptureUnusualVariantExtensions() {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getRequestURI()).thenReturn("/a/request/uri/.xml");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri/.xml"));
            
            final MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director);
            
            assertNotNull("Identified media type from request variant extensions should not be null", identifiedMediaType);
            assertEquals("xml", identifiedMediaType.getVariantExtension());
            assertEquals("/a/request/uri/", director.getRequestUri());
            assertEquals("http://localhost/a/request/uri/", director.getRequestUrl().toString());
        }
        
        @Test
        public void shouldSetCorrectMediaTypeWhenWildCardIsProvided(){
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getHeader(CommonHttpHeader.ACCEPT.toString())).thenReturn("*/*");
            when(request.getRequestURI()).thenReturn("/a/request/uri");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri"));
            normalizer.normalizeContentMediaType(request, director);
            
            assertTrue(director.requestHeaderManager().headersToAdd().keySet().contains(HeaderName.wrap("accept")));
            assertTrue(director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("accept")).contains("application/xml"));
        }
        
        @Test
        public void shouldNotSetMediaTypesWhenAcceptIsProvided(){
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            when(request.getHeader(CommonHttpHeader.ACCEPT.toString())).thenReturn("application/json");
            when(request.getRequestURI()).thenReturn("/a/request/uri");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri"));
            normalizer.normalizeContentMediaType(request, director);
            
            assertFalse(director.requestHeaderManager().headersToAdd().keySet().contains("accept"));
            assertTrue(director.requestHeaderManager().headersToAdd().isEmpty());
        }
        
        @Test
        public void shouldSetProperMediaTypeFromExtension(){
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final FilterDirector director = new FilterDirectorImpl();
            
            //when(request.getHeader(CommonHttpHeader.ACCEPT.toString())).thenReturn("application/json");
            when(request.getRequestURI()).thenReturn("/a/request/uri.xml");
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/a/request/uri.xml"));
            normalizer.normalizeContentMediaType(request, director);
            
            assertTrue(director.requestHeaderManager().headersToAdd().keySet().contains(HeaderName.wrap("accept")));
            assertTrue(director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("accept")).contains("application/xml"));
        }

        @Test
        public void should() {
            Pattern VARIANT_EXTRACTOR_REGEX = Pattern.compile("((\\.)[^\\d][\\w]*)");

            Matcher variantMatcher = VARIANT_EXTRACTOR_REGEX.matcher("http://localhost:8080/v1/test-service-mock-0.9.2-SNAPSHOT/whatever.xml");

            if (variantMatcher.find()) {
                for (int i = 1; i <=  variantMatcher.groupCount(); i++) {
                    System.out.println(variantMatcher.group(i));
                }
            }
        }
    }

    /**
     * Test of normalizeContentMediaType method, of class MediaTypeNormalizer.
     */
    @Test
    public void testNormalizeContentMediaType() {
        System.out.println("normalizeContentMediaType");
        HttpServletRequest request = null;
        FilterDirector director = null;
        MediaTypeNormalizer instance = null;
        instance.normalizeContentMediaType(request, director);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMediaTypeForVariant method, of class MediaTypeNormalizer.
     */
    @Test
    public void testGetMediaTypeForVariant() {
        System.out.println("getMediaTypeForVariant");
        HttpServletRequest request = null;
        FilterDirector director = null;
        MediaTypeNormalizer instance = null;
        MediaType expResult = null;
        MediaType result = instance.getMediaTypeForVariant(request, director);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of formatVariant method, of class MediaTypeNormalizer.
     */
    @Test
    public void testFormatVariant() {
        System.out.println("formatVariant");
        String variant = "";
        String expResult = "";
        String result = MediaTypeNormalizer.formatVariant(variant);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
