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
package org.openrepose.core.services.datastore.impl.distributed;

import org.openrepose.commons.utils.net.NetworkInterfaceProvider;
import org.openrepose.commons.utils.net.StaticNetworkInterfaceProvider;
import org.openrepose.core.services.datastore.distributed.ClusterView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.*;

public class ThreadSafeClusterView implements ClusterView {

   private static final Logger LOG = LoggerFactory.getLogger(ThreadSafeClusterView.class);
   private static final Comparator<ClusterMember> CLUSTER_MEMBER_COMPARATOR = new Comparator<ClusterMember>() {

      @Override
      public int compare(ClusterMember o1, ClusterMember o2) {
         final BigInteger o1Address = new BigInteger(o1.getMemberAddress().getAddress().getAddress());
         final BigInteger o2Address = new BigInteger(o2.getMemberAddress().getAddress().getAddress());

         return o1Address.compareTo(o2Address);
      }
   };
   private static final int DEFAULT_REST_DURATION_IN_MILISECONDS = 10000;
   private final NetworkInterfaceProvider networkInterfaceProvider;
   private final List<ClusterMember> clusterMembers;
   private final List<Integer> listenPorts;

   public ThreadSafeClusterView(List<Integer> listenPorts) {
      this(new LinkedList<ClusterMember>(), listenPorts);
   }

   public ThreadSafeClusterView(List<ClusterMember> clusterMembers, List<Integer> listenPorts) {
      this(StaticNetworkInterfaceProvider.getInstance(), new LinkedList<ClusterMember>(clusterMembers), listenPorts);
   }

   public ThreadSafeClusterView(NetworkInterfaceProvider networkInterfaceProvider, List<ClusterMember> clusterMembers, List<Integer> listenPorts) {
       this.networkInterfaceProvider = networkInterfaceProvider;
       this.clusterMembers = clusterMembers;
       this.listenPorts = listenPorts;
   }

   private static void normalizeClusterMembers(List<ClusterMember> members) {
      // Normalize the member order
      Collections.sort(members, CLUSTER_MEMBER_COMPARATOR);
   }

   @Override
   public ThreadSafeClusterView copy() {
      return new ThreadSafeClusterView(clusterMembers, listenPorts);
   }

   @Override
   public synchronized void memberDamaged(InetSocketAddress address, String reason) {
      for (ClusterMember member : clusterMembers) {
         if (member.getMemberAddress().equals(address)) {
            LOG.warn("Cluster member \"" + member.getMemberAddress().toString()
                    + "\" has been marked as damaged. We will retry this cluster "
                    + "member later. Reason: " + reason);

            member.setOffline();
            break;
         }
      }
   }

   @Override
   public synchronized void updateMembers(InetSocketAddress[] view) {
      clusterMembers.clear();

      for (InetSocketAddress memberAddress : view) {
         clusterMembers.add(new ClusterMember(memberAddress, DEFAULT_REST_DURATION_IN_MILISECONDS));
      }

      normalizeClusterMembers(clusterMembers);
   }

   @Override
   public void updateMembers(List<InetSocketAddress> view) {
      updateMembers(view.toArray(new InetSocketAddress[view.size()]));
   }

   @Override
   public synchronized InetSocketAddress[] members() {
      final LinkedList<InetSocketAddress> activeClusterMembers = new LinkedList<InetSocketAddress>();

      for (ClusterMember member : clusterMembers) {
         final boolean memberIsOnline = member.isOnline();

         if (memberIsOnline || member.shouldRetry()) {
            if (!memberIsOnline) {
               LOG.warn("Cluster member \"" + member.getMemberAddress().toString() + "\" was previously marked as damaged but is now eligible for retry.");
            }

            activeClusterMembers.add(member.getMemberAddress());
         }
      }

      return activeClusterMembers.toArray(new InetSocketAddress[activeClusterMembers.size()]);
   }

   @Override
   public synchronized boolean hasDamagedMembers() {
      for (ClusterMember member : clusterMembers) {
         if (!member.isOnline()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean isLocal(InetSocketAddress addr) throws SocketException {
      boolean havePort = false;
      for (Integer port : listenPorts) {
         if (addr.getPort() == port) {
            havePort = true;
            break;
         }
      }

      if (havePort && networkInterfaceProvider.hasInterfaceFor(addr.getAddress())) {
         return true;
      }

      return false;
   }

   /**
    * It was really annoying to create a clusterview for only one port all the time, so this wraps that
    * Returns a threadSafeClusterView that has been built with a list of only one port
    * @param port
    * @return
    */
   public static ThreadSafeClusterView singlePortClusterView(int port) {
      List<Integer> portList = new ArrayList<>();
      portList.add(port);
      return new ThreadSafeClusterView(portList);
   }

}
