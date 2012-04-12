package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import org.openrepose.components.routing.servlet.config.RootContextRouterConfiguration;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.model.ServiceDomain;
import java.util.ArrayList;
import java.util.List;
import com.rackspace.papi.domain.Port;


public class RoutingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RoutingTagger> {

   private RootContextRouterConfiguration contextRouterConfiguration;
   private final Map<String, Destination> configuredHosts = new HashMap<String, Destination>();
   private final List<Port> ports;
   private ServiceDomain localDomain;
   private DomainNode localHost;
   
    public RoutingHandlerFactory(List<Port> ports) {
        this.ports = ports;
    }
   
   

   private class RoutingConfigurationListener implements UpdateListener<RootContextRouterConfiguration> {

      @Override
      public void configurationUpdated(RootContextRouterConfiguration configurationObject) {
         contextRouterConfiguration = configurationObject;
      }
   }
   
   private class SystemModelConfigurationListener implements UpdateListener<PowerProxy> {

      @Override
      public void configurationUpdated(PowerProxy configurationObject) {
         SystemModelInterrogator interrogator = new SystemModelInterrogator(configurationObject, ports);
         localDomain = interrogator.getLocalServiceDomain();
         localHost = interrogator.getLocalHost();
         List<Destination> destinations = new  ArrayList<Destination>();
         destinations.addAll(localDomain.getDestinations().getEndpoint());
         destinations.addAll(localDomain.getDestinations().getTargetDomain());
         for (Destination powerApiHost : destinations) {
            configuredHosts.put(powerApiHost.getId(), powerApiHost);
         }
      }
   }

   @Override
   protected RoutingTagger buildHandler() {
      return new RoutingTagger(contextRouterConfiguration.getTarget(),configuredHosts);
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
      updateListeners.put(RootContextRouterConfiguration.class, new RoutingConfigurationListener());
      updateListeners.put(PowerProxy.class, new SystemModelConfigurationListener());
      return updateListeners;
   }
}
