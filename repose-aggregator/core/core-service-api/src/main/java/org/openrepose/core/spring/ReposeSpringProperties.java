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
package org.openrepose.core.spring;

/**
 * Used to provide a single point of reference for our necessary spring properties
 * Only for use in @Value annotations, else you have to take away the ${...} decorations
 */
public class ReposeSpringProperties {

    /**
     * Properties available in the core context, and to all child contexts
     */
    public static class CORE {
        public static final String REPOSE_VERSION = "${repose-version}";
        public static final String CONFIG_ROOT = "${powerapi-config-directory}";
        public static final String INSECURE = "${repose-insecurity}";
    }

    /**
     * Properties available in the per-node contexts, and all child contexts
     */
    public static class NODE {
        public static final String NODE_ID = "${repose-node-id}";
        public static final String CLUSTER_ID = "${repose-cluster-id}";
    }

    public static String stripSpringValueStupidity(String atValue) {
        return atValue.substring(2, atValue.length() - 1);
    }

}
