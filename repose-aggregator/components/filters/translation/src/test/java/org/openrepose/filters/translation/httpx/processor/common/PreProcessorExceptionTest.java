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
package org.openrepose.filters.translation.httpx.processor.common;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author kush5342
 */
@RunWith(Enclosed.class)
public class PreProcessorExceptionTest {
    
 public static class WhenException {
 
  @Test
        public void shouldProcessCustomMessage() {
            String expectedExceptionMessage = "Oops!  Something unexpected happened.";
             
            PreProcessorException preProcessorException =new PreProcessorException(expectedExceptionMessage);
             
            assertEquals(expectedExceptionMessage,preProcessorException.getMessage());
            
            String expectedExceptionMessage2 = "Oops!  Something unexpected happened again.";
            
            preProcessorException = new PreProcessorException(expectedExceptionMessage, new Throwable("unexpected"));

            assertEquals(expectedExceptionMessage, preProcessorException.getMessage());
           
   
             preProcessorException = new PreProcessorException(new Throwable("unexpected again"));
             
             assertEquals("unexpected again", preProcessorException.getCause().getMessage());
     
 }
 }
}
