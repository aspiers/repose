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
package org.openrepose.commons.utils.http.media.servlet;

import org.openrepose.commons.utils.http.header.HeaderValueImpl;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class RequestMediaRangeInterrogatorTest {
    public static class WhenInterrogatingRequests {
        
        @Test
        public void shouldReturnMediaTypeFromVariant() {
            List<MediaType> mediaRange = RequestMediaRangeInterrogator.interrogate("http://cloudservers/images.json", new HeaderValueImpl(""));

            assertEquals(MimeType.APPLICATION_JSON, mediaRange.get(0).getMimeType());
        }

        @Test
        public void shouldReturnMediaTypeFromAcceptHeader() {
            List<MediaType> mediaRange = RequestMediaRangeInterrogator.interrogate("http://servers.api.openstack.org/images", new HeaderValueImpl("application/xml"));

            assertEquals(MimeType.APPLICATION_XML, mediaRange.get(0).getMimeType());
        }
    }
}
