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
package org.openrepose.core.services.reporting.jmx;

import javax.management.openmbean.*;

public abstract class CompositeDataBuilder {

    public abstract String getItemName();

    public abstract String getDescription();

    public abstract String[] getItemNames();

    public abstract String[] getItemDescriptions();

    public abstract OpenType[] getItemTypes();

    public abstract Object[] getItems();

    public CompositeData toCompositeData() throws OpenDataException {
        return new CompositeDataSupport(getCompositeType(), getItemNames(), getItems());
    }

    private CompositeType getCompositeType() throws OpenDataException {
        return new CompositeType(getItemName(), getDescription(), getItemNames(), getItemDescriptions(), getItemTypes());
    }
}
