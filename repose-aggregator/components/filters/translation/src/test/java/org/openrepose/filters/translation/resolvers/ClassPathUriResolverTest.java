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
package org.openrepose.filters.translation.resolvers;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ClassPathUriResolverTest {

    public static class WhenResolveResources {
        private URIResolver parent;
        private ClassPathUriResolver resolver;

        @Before
        public void setUp() {
            parent = mock(URIResolver.class);
            resolver = new ClassPathUriResolver(parent);
        }
        
        @Test
        public void shouldFindResource() throws TransformerException {
            Source resource = resolver.resolve(ClassPathUriResolver.CLASSPATH_PREFIX + "/style.xsl", "");
            assertNotNull("Should find resource", resource);
            assertNotNull("Resource should contain the source path", resource.getSystemId());
            assertFalse("Resource path should not be empty", resource.getSystemId().isEmpty());
        }
        
        @Test
        public void shouldReturnNullWhenResourceNotFound() throws TransformerException {
            Source resource = resolver.resolve(ClassPathUriResolver.CLASSPATH_PREFIX + "/blah.xsl", "");
            assertNull("Should return null for non-existent resource", resource);
        }
        
        @Test
        public void shouldHandleNullHref() throws TransformerException {
            Source resource = resolver.resolve(null, "");
            assertNull("Should handle null href", resource);
        }
        
        @Test
        public void shouldCallParentResolverForNonClassPathResources() throws TransformerException {
            String href = "/style.xsl";
            String base = "base";
            Source resource = resolver.resolve(href, base);
            verify(parent).resolve(eq(href), eq(base));
        }
    }
}
