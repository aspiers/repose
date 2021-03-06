/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
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
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.deploy;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(Enclosed.class)
public class EarFilenameFilterTest {

    public static class WhenLocatingEarFile {

        protected File dir = new File("/usr/share/repose/filters");
        protected EarFilenameFilter earFilenameFilter;

        @Before
        public void setUp() {

            earFilenameFilter = (EarFilenameFilter) EarFilenameFilter.getInstance();
        }

        @Test
        public void shouldReturnTrueForValidEarName() {

            assertTrue(earFilenameFilter.accept(dir, "filter-bundle.ear"));
        }

        @Test
        public void shouldReturnFalseForInvalidEarName() {
            assertFalse(earFilenameFilter.accept(dir, "filter-bunder"));
        }

        @Test
        public void shouldReturnFalseForEmptyEarName() {
            assertFalse(earFilenameFilter.accept(dir, ""));
        }
    }
}
