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
package org.openrepose.core.services.serviceclient.akka;

import org.openrepose.commons.utils.http.ServiceClientResponse;

import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Interface to provide Akka and futures support to Auth client
 */
public interface AkkaServiceClient {

    ServiceClientResponse get(String token, String uri, Map<String, String> headers) throws AkkaServiceClientException;
    ServiceClientResponse post(String requestKey, String uri, Map<String, String> headers, String payload, MediaType contentMediaType) throws AkkaServiceClientException;
}
