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
package org.openrepose.commons.config.parser.inputstream;

import org.openrepose.commons.config.parser.common.AbstractConfigurationObjectParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.config.resource.ResourceResolutionException;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamConfigurationParser extends AbstractConfigurationObjectParser<InputStream> {

   public InputStreamConfigurationParser() {
      super(InputStream.class);
   }

   @Override
   public InputStream read(ConfigurationResource cr) {
      try {
         return cr.newInputStream();
      } catch (IOException ex) {
         throw new ResourceResolutionException("Unable to read configuration file: " + cr.name(), ex);
      }
   }
}
