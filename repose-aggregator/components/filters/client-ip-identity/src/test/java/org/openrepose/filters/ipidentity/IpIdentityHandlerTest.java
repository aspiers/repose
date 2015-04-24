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
package org.openrepose.filters.ipidentity;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.net.IpAddressRange;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.filters.ipidentity.config.IpIdentityConfig;
import org.openrepose.filters.ipidentity.config.WhiteList;

import javax.servlet.http.HttpServletRequest;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IpIdentityHandlerTest {

   private static String DEFAULT_IP_VALUE = "10.0.0.1";
   private static String WHITELIST_IP_VALUE = "10.0.0.1";
   private static Double WL_QUALITY = 0.2;
   private static String WL_QUALITY_VALUE = ";q=0.2";
   private static Double QUALITY = 0.2;
   private static String QUALITY_VALUE = ";q=0.2";
   private HttpServletRequest request;
   private ReadableHttpServletResponse response;
   private IpIdentityHandler handler;
   private IpIdentityConfig config;
   private IpIdentityHandlerFactory factory;

   @Before
   public void setUp() {
      request = mock(HttpServletRequest.class);
      response = mock(ReadableHttpServletResponse.class);
      factory = new IpIdentityHandlerFactory();

      when(request.getRemoteAddr()).thenReturn(DEFAULT_IP_VALUE);
   }

   /**
    * Test of handleRequest method, of class IpIdentityHandler.
    */
   @Test
   public void testHandleRequest() throws Exception {
      config = new IpIdentityConfig();
      config.setQuality(QUALITY);
      factory.configurationUpdated(config);
      handler = factory.buildHandler();

      FilterDirector director = handler.handleRequest(request, response);

      assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString())).contains(DEFAULT_IP_VALUE + QUALITY_VALUE));
      assertTrue("Should have IP_Standard as a group", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.GROUPS.toString())).contains(IpIdentityGroup.DEST_GROUP + QUALITY_VALUE));
   }
   
   private List<IpAddressRange> buildRanges(WhiteList list) {
      List<IpAddressRange> ranges = new ArrayList<IpAddressRange>();
      
      for (String address: list.getIpAddress()) {
         try {
            ranges.add(new IpAddressRange(address));
         } catch (UnknownHostException ex) {
         }
      }
      
      return ranges;
   }

   @Test
   public void shouldAddWhiteListGroupAndQuality() throws Exception {
      config = new IpIdentityConfig();
      config.setQuality(QUALITY);
      WhiteList whiteList = new WhiteList();
      whiteList.setQuality(WL_QUALITY);
      whiteList.getIpAddress().add(WHITELIST_IP_VALUE);
      config.setWhiteList(whiteList);
      //handler = new IpIdentityHandler(config, buildRanges(whiteList));
      
      factory.configurationUpdated(config);
      handler = factory.buildHandler();

      FilterDirector director = handler.handleRequest(request, response);

      assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString())).contains(DEFAULT_IP_VALUE + WL_QUALITY_VALUE));
      assertTrue("Should have IP_Super as a group", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.GROUPS.toString())).contains(IpIdentityGroup.DEFAULT_WHITELIST_GROUP + WL_QUALITY_VALUE));
   }

   @Test
   public void shouldUseXForwardedForHeaderWithWhitelistRange() throws Exception {
      final String IP = "192.168.1.1";
      final String NETWORK = "192.168.0.0/16";
      config = new IpIdentityConfig();
      config.setQuality(QUALITY);
      WhiteList whiteList = new WhiteList();
      whiteList.setQuality(WL_QUALITY);
      whiteList.getIpAddress().add(NETWORK);
      config.setWhiteList(whiteList);
      factory.configurationUpdated(config);
      handler = factory.buildHandler();
      
      when(request.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString())).thenReturn(IP);

      FilterDirector director = handler.handleRequest(request, response);

      assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString())).contains(IP + WL_QUALITY_VALUE));
      assertTrue("Should have IP_Super as a group", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.GROUPS.toString())).contains(IpIdentityGroup.DEFAULT_WHITELIST_GROUP + WL_QUALITY_VALUE));
   }

   @Test
   public void shouldUseXForwardedForHeader() throws Exception {
      final String IP = "192.169.1.1";
      final String NETWORK = "192.168.0.0/16";
      config = new IpIdentityConfig();
      config.setQuality(QUALITY);
      WhiteList whiteList = new WhiteList();
      whiteList.setQuality(WL_QUALITY);
      whiteList.getIpAddress().add(NETWORK);
      config.setWhiteList(whiteList);
      factory.configurationUpdated(config);
      handler = factory.buildHandler();
      
      when(request.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString())).thenReturn(IP);

      FilterDirector director = handler.handleRequest(request, response);

      assertTrue("Should have Requests Source IP as x-pp-user", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.USER.toString())).contains(IP + WL_QUALITY_VALUE));
      assertTrue("Should have IP_Standard as a group", director.requestHeaderManager().headersToAdd().get(HeaderName.wrap(PowerApiHeader.GROUPS.toString())).contains(IpIdentityGroup.DEST_GROUP + QUALITY_VALUE));
   }
}
