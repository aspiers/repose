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
package org.openrepose.filters.versioning.testhelpers;

import org.openrepose.commons.utils.http.CommonHttpHeader;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/10/11
 * Time: 12:58 PM
 */
public abstract class HttpServletRequestMockFactory {
    public static HttpServletRequest create(String requestUri, String requestUrl, String acceptHeader) {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

        when(httpServletRequest.getRequestURI()).thenReturn(requestUri);

        StringBuffer temp = new StringBuffer();
        temp.append(requestUrl);
        when(httpServletRequest.getRequestURL()).thenReturn(temp);

        when(httpServletRequest.getHeader(CommonHttpHeader.ACCEPT.toString())).thenReturn(acceptHeader);

        return httpServletRequest;
    }
}
