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
package org.openrepose.commons.utils.digest.impl;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class MD5MessageDigesterTest {

    private static final String SOURCE_DATA = "Lorem ipsum dolor sit amet";

    public static class WhenDigestingStreams {

        @Test
        public void shouldProduceCorrectDigest() {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(SOURCE_DATA.getBytes());

            final String expectedHex = "fea80f2db003d4ebc4536023814aa885";
            final String actualHex = HexHelper.convertToHex(new MD5MessageDigester().digestStream(inputStream));

            assertEquals("Digesting stream should produce expected MD5 hash",
                    expectedHex, actualHex);
        }
    }

    public static class WhenDigestingByteArrays {

        @Test
        public void shouldProduceCorrectDigest() {
            byte[] bytes = SOURCE_DATA.getBytes();

            final String expectedHex = "fea80f2db003d4ebc4536023814aa885";
            final String actualHex = HexHelper.convertToHex(new MD5MessageDigester().digestBytes(bytes));

            assertEquals("Digesting stream should produce expected MD5 hash",
                    expectedHex, actualHex);
        }
    }
}
