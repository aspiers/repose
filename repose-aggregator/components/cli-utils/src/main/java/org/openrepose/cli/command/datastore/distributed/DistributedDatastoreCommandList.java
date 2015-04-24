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
package org.openrepose.cli.command.datastore.distributed;

import org.openrepose.cli.command.AbstractCommandList;
import org.openrepose.cli.command.Command;

/**
 *
 * @author zinic
 */
public class DistributedDatastoreCommandList extends AbstractCommandList {

   @Override
   public String getCommandToken() {
      return "dist-datastore";
   }

   @Override
   public String getCommandDescription() {
      return "Commands related to managing the distributed datastore component";
   }

   @Override
   public Command[] availableCommands() {
      return new Command[] {
         new CacheKeyEncoder()
      };
   }
}
