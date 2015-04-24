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
package org.openrepose.filters.headernormalization;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.filters.headernormalization.config.HeaderFilterList;
import org.openrepose.filters.headernormalization.config.HeaderNormalizationConfig;
import org.openrepose.filters.headernormalization.config.Target;
import org.openrepose.filters.headernormalization.util.CompiledRegexAndList;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HeaderNormalizationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderNormalizationHandler> {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(HeaderNormalizationHandlerFactory.class);
    private final MetricsService metricsService;
    private List<CompiledRegexAndList> compiledTargetList;

    public HeaderNormalizationHandlerFactory(MetricsService metricsService) {
        compiledTargetList = new LinkedList<CompiledRegexAndList>();
        this.metricsService = metricsService;
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(HeaderNormalizationConfig.class, new ContentNormalizationConfigurationListener());
            }
        };
    }

    @Override
    protected HeaderNormalizationHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return new HeaderNormalizationHandler(compiledTargetList, metricsService);
    }

    private class ContentNormalizationConfigurationListener implements UpdateListener<HeaderNormalizationConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(HeaderNormalizationConfig configurationObject) {
            compiledTargetList.clear();
            final HeaderFilterList filterList = configurationObject.getHeaderFilters();
            CompiledRegexAndList compiledRegexAndList;
            if (filterList != null) {
                for (Target target : filterList.getTarget()) {
                    // TODO: Build objects with pre-compiled regexes and pass those to the Handler

                    if (target.getBlacklist().isEmpty()) {
                        compiledRegexAndList = new CompiledRegexAndList(target.getUriRegex(), target.getWhitelist().get(0).getHeader(), target.getHttpMethods(), Boolean.FALSE);
                    } else {
                        compiledRegexAndList = new CompiledRegexAndList(target.getUriRegex(), target.getBlacklist().get(0).getHeader(), target.getHttpMethods(), Boolean.TRUE);

                    }
                    compiledTargetList.add(compiledRegexAndList);
                }
            } else {
                LOG.warn("No Header List Configured for Header Normalizer Filter");
            }

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
