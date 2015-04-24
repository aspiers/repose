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
package org.openrepose.core.services;

import org.openrepose.commons.utils.http.ServiceClientResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public interface RequestProxyService {

    //TODO: this is the most terrible return value ever
    int proxyRequest(String targetHost, HttpServletRequest request, HttpServletResponse response) throws IOException;

    void setRewriteHostHeader(boolean value);

    ServiceClientResponse get(String uri, Map<String, String> headers);

    ServiceClientResponse get(String baseUri, String extraUri, Map<String, String> headers);

    ServiceClientResponse delete(String baseUri, String extraUri, Map<String, String> headers);

    ServiceClientResponse put(String uri, Map<String, String> headers, byte[] body);

    ServiceClientResponse put(String baseUri, String path, Map<String, String> headers, byte[] body);

    ServiceClientResponse patch(String baseUri, String path, Map<String, String> headers, byte[] body);
}
