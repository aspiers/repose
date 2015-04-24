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
package org.openrepose.cli.command.datastore.local;

import org.openrepose.core.services.datastore.distributed.impl.ehcache.ReposeLocalCacheMBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

public class ReposeJMXClient implements ReposeLocalCacheMBean {

    private final ReposeLocalCacheMBean reposeLocalCacheMBeanProxy;

    public ReposeJMXClient(String port) throws IOException, MalformedObjectNameException {

        final String jmxRmiUrl = "service:jmx:rmi:///jndi/rmi://:" + port + "/jmxrmi";
        final JMXServiceURL url = new JMXServiceURL(jmxRmiUrl);
        final JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        final MBeanServerConnection reposeConnection = jmxc.getMBeanServerConnection();

        reposeLocalCacheMBeanProxy = JMX.newMBeanProxy(reposeConnection,
                                                       new ObjectName(ReposeLocalCacheMBean.OBJECT_NAME),
                                                       ReposeLocalCacheMBean.class,
                                                       true);
    }

    @Override
    public boolean removeTokenAndRoles(String tenantId, String token) {
        return reposeLocalCacheMBeanProxy.removeTokenAndRoles(tenantId, token);
    }

    @Override
    public boolean removeGroups(String tenantId, String token) {
        return reposeLocalCacheMBeanProxy.removeGroups(tenantId, token);
    }

    @Override
    public boolean removeLimits(String encodedUserId) {
        return reposeLocalCacheMBeanProxy.removeLimits(encodedUserId);
    }

    @Override
    public void removeAllCacheData() {
        reposeLocalCacheMBeanProxy.removeAllCacheData();
    }
}
