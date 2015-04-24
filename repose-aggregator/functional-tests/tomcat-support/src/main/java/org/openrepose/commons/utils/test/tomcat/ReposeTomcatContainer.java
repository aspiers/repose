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
package org.openrepose.commons.utils.test.tomcat;

import org.openrepose.commons.utils.test.ReposeContainer;
import org.openrepose.commons.utils.test.ReposeContainerProps;
import org.openrepose.commons.utils.test.mocks.util.MocksUtil;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;

public class ReposeTomcatContainer extends ReposeContainer {
    private static final Logger LOG = LoggerFactory.getLogger(ReposeTomcatContainer.class);

    private Tomcat tomcat;
    private static final String BASE_DIRECTORY = System.getProperty("java.io.tmpdir");


    public ReposeTomcatContainer(ReposeContainerProps props) throws ServletException {
        super(props);
        tomcat = new Tomcat();
        tomcat.setBaseDir(BASE_DIRECTORY);
        tomcat.setPort(Integer.parseInt(listenPort));
        tomcat.getHost().setAutoDeploy(true);
        tomcat.getHost().setDeployOnStartup(true);
        tomcat.addWebapp("/", warLocation).setCrossContext(true);

        if(props.getOriginServiceWars() != null && props.getOriginServiceWars().length != 0){
            for(String originService: props.getOriginServiceWars()){
                tomcat.addWebapp("/"+ MocksUtil.getServletPath(originService), originService);
            }
        }
    }

    @Override
    protected void startRepose() {
        try {
            tomcat.start();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    stopRepose();
                }
            });

            System.out.println("Tomcat Container Running");
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            LOG.trace("Unable To Start Tomcat Server", e);
        }
    }

    @Override
    protected void stopRepose() {
        try {
            System.out.println("Stopping Tomcat Server");
            tomcat.stop();
            tomcat.getServer().stop();
        } catch (LifecycleException e) {
            LOG.trace("Error stopping Repose Tomcat", e);
        }
    }
}
