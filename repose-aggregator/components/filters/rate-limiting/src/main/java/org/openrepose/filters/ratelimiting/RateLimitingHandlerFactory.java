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
package org.openrepose.filters.ratelimiting;

import com.google.common.base.Optional;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.filters.ratelimiting.write.ActiveLimitsWriter;
import org.openrepose.filters.ratelimiting.write.CombinedLimitsWriter;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.ratelimit.RateLimitingService;
import org.openrepose.core.services.ratelimit.RateLimitingServiceFactory;
import org.openrepose.core.services.ratelimit.cache.ManagedRateLimitCache;
import org.openrepose.core.services.ratelimit.cache.RateLimitCache;
import org.openrepose.core.services.ratelimit.config.DatastoreType;
import org.openrepose.core.services.ratelimit.config.RateLimitingConfiguration;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/* Responsible for creating rate limit handlers that provide datastoreservice and listener to rate limit configuration */
public class RateLimitingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RateLimitingHandler> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingHandlerFactory.class);
    private static final String DEFAULT_DATASTORE_NAME = "local/default";

    private RateLimitCache rateLimitCache;
    //Volatile
    private Optional<Pattern> describeLimitsUriRegex;
    private RateLimitingConfiguration rateLimitingConfig;
    private RateLimitingService service;
    private final DatastoreService datastoreService;

    public RateLimitingHandlerFactory(DatastoreService datastoreService) {
        this.datastoreService = datastoreService;

    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> listenerMap = new HashMap<Class, UpdateListener<?>>();
        listenerMap.put(RateLimitingConfiguration.class, new RateLimitingConfigurationListener());

        return listenerMap;
    }

    private Datastore getDatastore(DatastoreType datastoreType) {
        Datastore targetDatastore;

        String requestedDatastore = datastoreType.value();

        if (StringUtilities.isNotBlank(requestedDatastore)) {
            LOG.info("Requesting datastore " + datastoreType);
            final Datastore datastore;

            if (requestedDatastore.equals(DEFAULT_DATASTORE_NAME)) {
                return datastoreService.getDefaultDatastore();
            }

            datastore = datastoreService.getDatastore(requestedDatastore);

            if (datastore != null) {
                LOG.info("Using requested datastore " + requestedDatastore);
                return datastore;
            }

            LOG.warn("Requested datastore not found");
        }

        targetDatastore = datastoreService.getDistributedDatastore();
        if (targetDatastore != null) {
            LOG.info("Using distributed datastore " + targetDatastore.getName());
        } else {
            LOG.warn("There were no distributed datastore managers available. Clustering for rate-limiting will be disabled.");
            targetDatastore = datastoreService.getDefaultDatastore();
        }

        return targetDatastore;
    }

    private class RateLimitingConfigurationListener implements UpdateListener<RateLimitingConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(RateLimitingConfiguration configurationObject) {

            rateLimitCache = new ManagedRateLimitCache(getDatastore(configurationObject.getDatastore()));

            service = RateLimitingServiceFactory.createRateLimitingService(rateLimitCache, configurationObject);

            if (configurationObject.getRequestEndpoint() != null) {
                describeLimitsUriRegex = Optional.of(Pattern.compile(configurationObject.getRequestEndpoint().getUriRegex()));
            } else {
                describeLimitsUriRegex = Optional.absent();
            }

            rateLimitingConfig = configurationObject;

            isInitialized = true;

        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected RateLimitingHandler buildHandler() {

        if (!this.isInitialized()) {
            return null;
        }

        final ActiveLimitsWriter activeLimitsWriter = new ActiveLimitsWriter();
        final CombinedLimitsWriter combinedLimitsWriter = new CombinedLimitsWriter();
        final RateLimitingServiceHelper serviceHelper = new RateLimitingServiceHelper(service, activeLimitsWriter, combinedLimitsWriter);

        boolean includeAbsoluteLimits = false;
        if (rateLimitingConfig.getRequestEndpoint() != null) {
            includeAbsoluteLimits = rateLimitingConfig.getRequestEndpoint().isIncludeAbsoluteLimits();
        }

        return new RateLimitingHandler(serviceHelper, includeAbsoluteLimits, describeLimitsUriRegex, rateLimitingConfig.isOverLimit429ResponseCode(), rateLimitingConfig.getDatastoreWarnLimit().intValue());
    }
}
