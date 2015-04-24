package org.openrepose.filters.translation.httpx.processor.json.elements;


import org.junit.Test;
import org.xml.sax.ContentHandler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 *
 * @author kush5342
 */
public class ScalarElementTest {
    
  
    /**
     * Test of outputElement method, of class ScalarElement.
     */
    @Test
    public void testOutputElement() throws Exception {
        ContentHandler handler = mock(ContentHandler.class);
        ScalarElement instance = new ScalarElement(BaseElement.JSONX_URI,"fid","value");
        instance.outputElement(handler);
        assertEquals("fid", instance.getAttributes().getValue(0));
    }
}
