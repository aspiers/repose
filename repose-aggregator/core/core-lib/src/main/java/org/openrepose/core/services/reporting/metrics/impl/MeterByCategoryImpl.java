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
package org.openrepose.core.services.reporting.metrics.impl;

import org.openrepose.core.services.reporting.metrics.MeterByCategory;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import com.yammer.metrics.core.Meter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implements a collection of Meters which share the same JMX type & scope.  These Meters are usually related in some
 * fashion.
 * <p>
 * By calling the mark() methods, a Meter object is automatically registered and can be marked by later calls.
 * <p>
 * This is created by the {@link org.openrepose.core.services.reporting.metrics.impl.MetricsServiceImpl} factory class.
 * <p>
 * This class is thread-safe.
 *
 */
public class MeterByCategoryImpl implements MeterByCategory {

    private Map<String, Meter> map = new HashMap<String, Meter>();
    private MetricsService metricsService;
    private Class klass;
    private String scope;
    private String eventType;
    private TimeUnit unit;


    MeterByCategoryImpl( MetricsService metricsServiceP, Class klassP, String scopeP, String eventTypeP,
                         TimeUnit unitP ) {

        metricsService = metricsServiceP;
        klass = klassP;
        scope = scopeP;
        eventType = eventTypeP;
        unit = unitP;
    }

    @Override
    public void mark( String key ) {

        verifyGet( key ).mark();
    }

    private  Meter verifyGet( String key ) {
        if ( !map.containsKey( key ) ) {
            synchronized ( this ) {
                if (!map.containsKey(key)) {
                    map.put(key, metricsService.newMeter(klass, key, scope, eventType, unit));
                }
            }
        }
        return map.get( key );
    }

    @Override
    public void mark( String key, long n ) {

        verifyGet( key ).mark( n );
    }
}
