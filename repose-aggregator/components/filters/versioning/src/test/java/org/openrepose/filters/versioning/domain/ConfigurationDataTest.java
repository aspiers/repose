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
package org.openrepose.filters.versioning.domain;

import org.openrepose.core.systemmodel.*;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.filters.versioning.config.MediaTypeList;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;
import org.openrepose.filters.versioning.config.ServiceVersionMappingList;
import org.openrepose.filters.versioning.schema.VersionChoiceList;
import org.openrepose.filters.versioning.util.http.HttpRequestInfo;
import org.openrepose.filters.versioning.util.http.UniformResourceInfo;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author malconis
 */
public class ConfigurationDataTest {

   ConfigurationData configurationData;
   Map<String, Destination> configuredHosts;
   Map<String, ServiceVersionMapping> configuredMappings;
   ServiceVersionMappingList mappings;
   ServiceVersionMapping version1, version2;
   Node localHost;
   DestinationEndpoint localEndpoint;
   ReposeCluster domain;

   @Before
   public void setUp() {
      domain = new ReposeCluster();
      domain.setFilters(mock(FilterList.class));

      localHost = new Node();
      localHost.setHostname("localhost");
      localHost.setHttpPort(8080);
      localHost.setHttpsPort(0);
      localHost.setId("localhost");

      localEndpoint = new DestinationEndpoint();
      localEndpoint.setHostname("localhost");
      localEndpoint.setPort(8080);
      localEndpoint.setProtocol("http");
      localEndpoint.setId("localhost");

      version1 = new ServiceVersionMapping();
      version1.setId("/v1");
      version1.setPpDestId("localhost");
      MediaTypeList v1MediaTypeList = new MediaTypeList();
      org.openrepose.filters.versioning.config.MediaType v1MediaType1 = new org.openrepose.filters.versioning.config.MediaType();
      v1MediaType1.setBase("application/xml");
      v1MediaType1.setType("application/vnd.vendor.service-v1+xml");
      v1MediaTypeList.getMediaType().add(v1MediaType1);
      version1.setMediaTypes(v1MediaTypeList);

      version2 = new ServiceVersionMapping();
      version2.setId("/v2");
      version2.setPpDestId("localhost");
      MediaTypeList v2MediaTypeList = new MediaTypeList();
      org.openrepose.filters.versioning.config.MediaType v2MediaType1 = new org.openrepose.filters.versioning.config.MediaType();
      v2MediaType1.setBase("application/xml");
      v2MediaType1.setType("application/vnd.vendor.service-v2+xml");
      v2MediaTypeList.getMediaType().add(v2MediaType1);
      version2.setMediaTypes(v1MediaTypeList);

      mappings = new ServiceVersionMappingList();

      mappings.getVersionMapping().add(version1);
      mappings.getVersionMapping().add(version2);

      configuredHosts = new HashMap<String, Destination>();
      configuredMappings = new HashMap<String, ServiceVersionMapping>();

      configuredHosts.put(localHost.getId(), localEndpoint);
      configuredMappings.put(version1.getId(), version1);
      configuredMappings.put(version2.getId(), version2);

      configurationData = new ConfigurationData(domain, localHost, configuredHosts, configuredMappings);

   }

   @Test
   public void shouldReturnHostForServiceMapping() throws VersionedHostNotFoundException {
      assertEquals(localEndpoint, configurationData.getHostForVersionMapping(version1));
   }

   @Test
   public void shouldReturnConfiguredHosts() {
      assertEquals(configuredHosts, configurationData.getConfiguredHosts());
   }

   @Test
   public void shouldReturnVersionedOriginServiceFromURI() throws VersionedHostNotFoundException {
      HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
      FilterDirector director = mock(FilterDirector.class);

      when(requestInfo.getUri()).thenReturn("/v1/service/rs");

      VersionedOriginService destination = configurationData.getOriginServiceForRequest(requestInfo, director);
      assertEquals("Should find proper host given a matched uri to a version mapping", localEndpoint, destination.getOriginServiceHost());
   }

   @Test
   public void shouldReturnVersionedOriginServiceFromAcceptHeader() throws VersionedHostNotFoundException {
      HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
      FilterDirector director = new FilterDirectorImpl();
      MediaType preferedMediaRange = new MediaType("application/vnd.vendor.service-v1+xml", MimeType.APPLICATION_XML);
      when(requestInfo.getUri()).thenReturn("/service/rs");
      when(requestInfo.getPreferedMediaRange()).thenReturn(preferedMediaRange);

      VersionedOriginService destination = configurationData.getOriginServiceForRequest(requestInfo, director);
      assertEquals("Should find proper host given a matched uri to a version mapping", localEndpoint, destination.getOriginServiceHost());
   }

   @Test
   public void shouldReturnVersionChoicesAsList() {
      HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
      MediaType preferedMediaRange = new MediaType("application/vnd.vendor.service-v1+xml", MimeType.APPLICATION_XML, -1);
      when(requestInfo.getUri()).thenReturn("/v1");
      when(requestInfo.getPreferedMediaRange()).thenReturn(preferedMediaRange);

      VersionChoiceList versionChoiceList = new VersionChoiceList();
      versionChoiceList = configurationData.versionChoicesAsList(requestInfo);

      assertEquals("Should return a version choice list of the two configured versions", versionChoiceList.getVersion().size(), 2);

   }

   @Test
   public void shouldReturnIfRequestIsForVersions() {
      UniformResourceInfo uniformResourceInfo = mock(UniformResourceInfo.class);
      when(uniformResourceInfo.getUri()).thenReturn("/");

      assertTrue("Should return true that this request is for the service root", configurationData.isRequestForVersions(uniformResourceInfo));
   }

   @Test
   public void shouldReturnNullIfNoMatchForMediaRangeIsFound() throws VersionedHostNotFoundException {

      HttpRequestInfo requestInfo = mock(HttpRequestInfo.class);
      FilterDirector director = new FilterDirectorImpl();
      MediaType preferedMediaRange = new MediaType("application/vnd.vendor.service-v3+xml", MimeType.APPLICATION_XML, -1);
      when(requestInfo.getUri()).thenReturn("/service/rs");
      when(requestInfo.getPreferedMediaRange()).thenReturn(preferedMediaRange);

      assertNull("Should find proper host given a matched uri to a version mapping", configurationData.getOriginServiceForRequest(requestInfo, director));
   }
}
