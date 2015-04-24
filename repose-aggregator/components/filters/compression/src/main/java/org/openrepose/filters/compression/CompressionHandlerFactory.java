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
package org.openrepose.filters.compression;

import org.openrepose.external.pjlcompression.CompressingFilter;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.filters.compression.utils.CompressionConfigWrapper;
import org.openrepose.filters.compression.utils.CompressionParameters;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.Map;

public class CompressionHandlerFactory extends AbstractConfiguredFilterHandlerFactory<CompressionHandler> {

   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CompressionHandlerFactory.class);
   private CompressionConfigWrapper config;
   private CompressingFilter filter;

   public CompressionHandlerFactory(FilterConfig config) {

      this.config = new CompressionConfigWrapper(config);
      this.config.setInitParameter(CompressionParameters.STATS_ENABLED.getParam(), "true");
      this.config.setInitParameter(CompressionParameters.JAVA_UTIL_LOGGER.getParam(), LOG.getName());
      this.config.setInitParameter(CompressionParameters.DEBUG.getParam(), "true");
      try {

         filter = new CompressingFilter();
         filter.init(config);
      } catch (ServletException ex) {
         LOG.error("Unable to initialize CompressingFilter: ", ex);
      }
   }

   private class ContentCompressionConfigurationListener implements UpdateListener<ContentCompressionConfig> {

      private boolean isInitialized = false;

      @Override
      public void configurationUpdated(ContentCompressionConfig configurationObject) {
         Compression contentCompressionConfig = configurationObject.getCompression();

         config.setInitParameter(CompressionParameters.STATS_ENABLED.getParam(), String.valueOf(contentCompressionConfig.isStatsEnabled()));
         config.setInitParameter(CompressionParameters.JAVA_UTIL_LOGGER.getParam(), LOG.getName());
         config.setInitParameter(CompressionParameters.DEBUG.getParam(), String.valueOf(contentCompressionConfig.isDebug()));
         config.setInitParameter(CompressionParameters.COMPRESSION_THRESHHOLD.getParam(), String.valueOf(contentCompressionConfig.getCompressionThreshold()));

         if (!contentCompressionConfig.getIncludeContentTypes().isEmpty()) {
            config.setInitParameter(CompressionParameters.INCLUDE_CONTENT_TYPES.getParam(), StringUtils.collectionToCommaDelimitedString(contentCompressionConfig.getIncludeContentTypes()));

         }else if (!contentCompressionConfig.getExcludeContentTypes().isEmpty()) {
            config.setInitParameter(CompressionParameters.EXCLUDE_CONTENT_TYPES.getParam(), StringUtils.collectionToCommaDelimitedString(contentCompressionConfig.getExcludeContentTypes()));

         }

         if (!contentCompressionConfig.getIncludeUserAgentPatterns().isEmpty()) {
            config.setInitParameter(CompressionParameters.INCLUDE_USER_AGENT_PATTERNS.getParam(), StringUtils.collectionToCommaDelimitedString(contentCompressionConfig.getIncludeUserAgentPatterns()));

         } else if (!contentCompressionConfig.getExcludeUserAgentPatterns().isEmpty()) {
            config.setInitParameter(CompressionParameters.EXCLUDE_USER_AGENT_PATTERNS.getParam(), StringUtils.collectionToCommaDelimitedString(contentCompressionConfig.getExcludeUserAgentPatterns()));
         }

         filter = new CompressingFilter();
         try {
            filter.init(config);
            filter.setForRepose();
            isInitialized = true;
         } catch (ServletException ex) {
            LOG.error("Unable to initialize content compression filter", ex);
         }
      }

      @Override
      public boolean isInitialized() {
         return isInitialized;
      }
   }

   @Override
   protected CompressionHandler buildHandler() {

      if (!this.isInitialized()) {
         return null;
      } else {
         return new CompressionHandler(filter);
      }
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      return new HashMap<Class, UpdateListener<?>>() {
         {
            put(ContentCompressionConfig.class, new ContentCompressionConfigurationListener());
         }
      };
   }
}
