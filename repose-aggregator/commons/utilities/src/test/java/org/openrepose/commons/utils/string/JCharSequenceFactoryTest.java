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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author kush5342
 */
public class JCharSequenceFactoryTest {
    
   

    /**
     * Test of jchars method, of class JCharSequenceFactory.
     */
    @Test
    public void testJchars_String() {
         String st = "test";
      
         JCharSequence result = JCharSequenceFactory.jchars(st);
         assertEquals("test", result.toString());
       
    }

    /**
     * Test of jchars method, of class JCharSequenceFactory.
     */
    @Test
    public void testJchars_StringBuffer() {
        
        StringBuffer sb = new StringBuffer("test");        
        JCharSequence result = JCharSequenceFactory.jchars(sb);
        assertEquals("test", result.asCharSequence().toString());
        
    }

    /**
     * Test of jchars method, of class JCharSequenceFactory.
     */
    @Test
    public void testJchars_StringBuilder() {
       
        StringBuilder sb = new StringBuilder("Test");
        JCharSequence expResult = null;
        JCharSequence result = JCharSequenceFactory.jchars(sb);
        assertEquals("Test", result.asCharSequence().toString());
        
    }
}
