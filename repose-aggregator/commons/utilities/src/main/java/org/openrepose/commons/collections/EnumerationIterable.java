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
package org.openrepose.commons.collections;

import java.util.Enumeration;
import java.util.Iterator;

/**
 *
 * @author zinic
 */
public class EnumerationIterable<T> implements Iterable<T> {

   private final EnumerationIterator<T> enumerationIterator;

   public EnumerationIterable(Enumeration<T> enumeration) {
      this.enumerationIterator = new EnumerationIterator<T>(enumeration);
   }

   @Override
   public Iterator<T> iterator() {
      return enumerationIterator;
   }
}
