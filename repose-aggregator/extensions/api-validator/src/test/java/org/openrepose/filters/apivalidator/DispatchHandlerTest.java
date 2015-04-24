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
package org.openrepose.filters.apivalidator;

import com.rackspace.com.papi.components.checker.handler.ResultHandler;
import com.rackspace.com.papi.components.checker.servlet.CheckerServletRequest;
import com.rackspace.com.papi.components.checker.servlet.CheckerServletResponse;
import com.rackspace.com.papi.components.checker.step.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import scala.Option;

import javax.servlet.FilterChain;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Enclosed.class)
public class DispatchHandlerTest {

    public static class WhenWrappingResultHandlers {
        private ResultHandler handler1;
        private ResultHandler handler2;
        private DispatchHandler instance;

        @Before
        public void setup() {
            this.handler1 = mock(ResultHandler.class);
            this.handler2 = mock(ResultHandler.class);
            this.instance = new DispatchHandler(handler1, handler2);
        }

        @Test
        public void shouldCallInitOnEachHandler() {
            Option<Document> option = mock(Option.class);
            instance.init(null, option);
            verify(handler1).init(null, option);
            verify(handler2).init(null, option);
        }

        @Test
        public void shouldCallHandleOnEachHandler() {
            CheckerServletRequest request = mock(CheckerServletRequest.class);
            CheckerServletResponse response = mock(CheckerServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            Result result = mock(Result.class);

            instance.handle(request, response, chain, result);
            verify(handler1).handle(request, response, chain, result);
            verify(handler2).handle(request, response, chain, result);
        }

        @Test
        public void shouldCallHandleWithPreviousStepHandler() {
            CheckerServletRequest request = mock(CheckerServletRequest.class);
            CheckerServletResponse response = mock(CheckerServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            Result result = mock(Result.class);

            instance.handle(request, response, chain, result);
            verify(handler1).handle(request, response, chain, result);
            verify(handler2).handle(request, response, chain, result);
        }

        @Test
        public void shouldHandleNullHandlerList() {
            DispatchHandler instance = new DispatchHandler(null);
            instance.init(null, null);
            instance.handle(null, null, null, null);
            instance.inStep(null, null, null, null);
        }

        @Test
        public void shouldHandleEmptyHandlerList() {
            DispatchHandler instance = new DispatchHandler(new ResultHandler[0]);
            instance.init(null, null);
            instance.handle(null, null, null, null);
            instance.inStep(null, null, null, null);
        }
    }
}
