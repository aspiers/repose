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
package org.openrepose.commons.utils.transform.xslt;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

public abstract class AbstractXslTransform {

   private final ObjectPool<Transformer> xsltResourcePool;
   private final Templates transformationTemplates;

   public AbstractXslTransform(Templates transformTemplates) {
      this.transformationTemplates = transformTemplates;

      xsltResourcePool = new SoftReferenceObjectPool<>(new BasePoolableObjectFactory<Transformer>() {

         @Override
         public Transformer makeObject() {
            try {
               return transformationTemplates.newTransformer();
            } catch (TransformerConfigurationException configurationException) {
               throw new XsltTransformationException("Failed to generate XSLT transformer. Reason: "
                       + configurationException.getMessage(), configurationException);
            }
         }
      });
   }

   protected ObjectPool<Transformer> getXslTransformerPool() {
      return xsltResourcePool;
   }
}
