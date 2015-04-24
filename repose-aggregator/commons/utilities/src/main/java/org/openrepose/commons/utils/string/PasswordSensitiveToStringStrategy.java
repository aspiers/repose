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
package org.openrepose.commons.utils.string;

import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

/**
 * @author fran
 */
public class PasswordSensitiveToStringStrategy extends JAXBToStringStrategy implements ToStringStrategy {

   private static final String PASSWORD_FIELD_NAME = "password";

   @Override
   public StringBuilder appendField(ObjectLocator objectLocator, Object o, String s, StringBuilder stringBuilder, Object o1) {
      if (PASSWORD_FIELD_NAME.equalsIgnoreCase(s)) {
         return super.appendField(objectLocator, o, PASSWORD_FIELD_NAME, stringBuilder, "*******");
      } else {
         return super.appendField(objectLocator, o, s, stringBuilder, o1);
      }
   }
}
