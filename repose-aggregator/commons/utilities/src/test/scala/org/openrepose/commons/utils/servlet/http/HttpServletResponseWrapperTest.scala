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
package org.openrepose.commons.utils.servlet.http

import java.io.ByteArrayInputStream
import javax.servlet.http.HttpServletResponse

import com.mockrunner.mock.web.MockHttpServletResponse
import org.apache.http.client.utils.DateUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class HttpServletResponseWrapperTest extends FunSpec with BeforeAndAfter with Matchers {

  // Think of: casing, preceding/succeeding components, ordering, quality

  var originalResponse: HttpServletResponse = _

  before {
    originalResponse = new MockHttpServletResponse()
  }

  describe("getHeaderNames") {
    it("should not return any header names added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getHeaderNames should not contain "a"
    }

    it("should return all header names added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.getHeaderNames should contain only("a", "b")
    }

    it("should not return any header names removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeaderNames should contain only "b"
    }

    it("should return header names with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a")
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.getHeaderNames should contain only("A", "b")
    }
  }

  describe("getPreferredHeaders") {
    it("should throw a QualityFormatException if the quality is not parseable") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a;q=fish")

      a[QualityFormatException] should be thrownBy wrappedResponse.getPreferredHeaders("a")
    }

    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getPreferredHeaders("a") should be('empty)
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a", 0.8)
      wrappedResponse.addHeader("a", "b", 0.8)
      wrappedResponse.addHeader("a", "c", 0.4)

      wrappedResponse.getPreferredHeaders("a") should contain only("a", "b")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getPreferredHeaders("a") should be('empty)
    }

    it("should not return query parameters in the header value") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar")
      wrappedResponse.addHeader("A", "b;bar=baz")

      wrappedResponse.getPreferredSplittableHeaders("A") should contain only "b"
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getPreferredHeaders("a") should contain only("a", "B")
    }
  }

  describe("getHeaderNamesList") {
    it("should not return any header names added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getHeaderNamesList should not contain "a"
    }

    it("should return all header names added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.getHeaderNamesList should contain only("a", "b")
    }

    it("should not return any header names removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeaderNamesList should contain only "b"
    }

    it("should return header names with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a")
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.getHeaderNamesList should contain only("A", "b")
    }
  }

  describe("containsHeader") {
    it("should not contain any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.containsHeader("a") shouldBe false
    }

    it("should contain all headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.containsHeader("a") shouldBe true
      wrappedResponse.containsHeader("b") shouldBe true
    }

    it("should not contain any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("b", "b")
      wrappedResponse.removeHeader("a")

      wrappedResponse.containsHeader("a") shouldBe false
      wrappedResponse.containsHeader("b") shouldBe true
    }

    it("should contain headers irrespective of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a")

      wrappedResponse.containsHeader("A") shouldBe true
      wrappedResponse.containsHeader("a") shouldBe true
    }
  }

  describe("getHeader") {
    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getHeader("a") shouldBe null
    }

    it("should return a header added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")

      wrappedResponse.getHeader("a") shouldEqual "a"
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeader("a") shouldBe null
    }

    it("should return the full header value, with query parameters included") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar")

      wrappedResponse.getHeader("A") shouldEqual "a;q=0.8;foo=bar"
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a")
      wrappedResponse.addHeader("b", "B")

      wrappedResponse.getHeader("A") shouldEqual "a"
      wrappedResponse.getHeader("b") shouldEqual "B"
    }

    it("should return the first header value if multiple exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getHeader("a") shouldEqual "a"
    }
  }

  describe("getHeaders") {
    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getHeaders("a") should be('empty)
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getHeaders("a") should contain only("a", "b")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeaders("a") should be('empty)
    }

    it("should return the full headers value, with query parameters included") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar")
      wrappedResponse.addHeader("A", "b;bar=baz")

      wrappedResponse.getHeaders("A") should contain only("a;q=0.8;foo=bar", "b;bar=baz")
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getHeaders("a") should contain only("a", "B")
    }
  }

  describe("getSplittableHeaders") {
    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getSplittableHeaders("a") should be('empty)
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getSplittableHeaders("a") should contain only("a", "b")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getSplittableHeaders("a") should be('empty)
    }

    it("should return the full header values, with query parameters included") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar,b;bar=baz")

      wrappedResponse.getSplittableHeaders("A") should contain only("a;q=0.8;foo=bar", "b;bar=baz")
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getSplittableHeaders("a") should contain only("a", "B")
    }

    it("should return a comma-serparated header value as multiple header values") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a,b")

      wrappedResponse.getSplittableHeaders("a").size() shouldEqual 2
      wrappedResponse.getSplittableHeaders("a") should (contain("a") and contain("b"))
    }
  }

  describe("getPreferredSplittableHeaders") {
    it("should throw a QualityFormatException if the quality is not parseable") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a;q=fish")

      a[QualityFormatException] should be thrownBy wrappedResponse.getPreferredHeaders("a")
    }

    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getPreferredSplittableHeaders("a") should be('empty)
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getPreferredSplittableHeaders("a") should contain only("a", "b")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getPreferredSplittableHeaders("a") should be('empty)
    }

    it("should not return query parameters in the header value") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("A", "a;q=0.8;foo=bar,b;bar=baz")

      wrappedResponse.getPreferredSplittableHeaders("A") should contain only "b"
    }

    it("should return split headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a", 0.8)
      wrappedResponse.appendHeader("a", "b", 0.8)
      wrappedResponse.appendHeader("a", "c", 0.4)

      wrappedResponse.getPreferredSplittableHeaders("a") should contain only("a", "b")
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getPreferredSplittableHeaders("a") should contain only("a", "B")
    }

    it("should return a comma-serparated header value as multiple header values") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a,b")

      wrappedResponse.getPreferredSplittableHeaders("a").size() shouldEqual 2
      wrappedResponse.getPreferredSplittableHeaders("a") should (contain("a") and contain("b"))
    }
  }

  describe("addHeader") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.addHeader("a", "a")
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.addHeader("a", "a")
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("b", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getHeaders("a") should contain only "b"
    }

    it("should add a value to a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("a", "b")
    }

    it("should add a value to a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("A", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("a", "b")
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.addHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")
      wrappedResponse.addHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header with a quality when provided") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("b", "b", 0.5)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b;q=0.5"
    }

    it("should add a header with a quality, even if the quality is 1.0") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("b", "b", 1.0)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b;q=1.0"
    }
  }

  describe("addIntHeader") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.addIntHeader("a", 1)
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.addIntHeader("a", 1)
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("b", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "2"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("a", 1)

      wrappedResponse.getHeaders("a") should contain only "1"
    }

    it("should add a value to a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("a", 1)
      wrappedResponse.addIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("1", "2")
    }

    it("should add a value to a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("a", 1)
      wrappedResponse.addIntHeader("A", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("1", "2")
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.addIntHeader("a", 1)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "1"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addIntHeader("a", 1)
      wrappedResponse.removeHeader("a")
      wrappedResponse.addIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "2"
    }
  }

  describe("addDateHeader") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.addDateHeader("a", System.currentTimeMillis())
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.addDateHeader("a", System.currentTimeMillis())
    }

    it("should add a new header, in a RFC2616 compliant format, if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      val now = System.currentTimeMillis()

      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.commitToResponse()

      DateUtils.parseDate(originalResponse.getHeader("a")) should not be null
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      val now = System.currentTimeMillis()

      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 2
    }

    it("should add a value to a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.addDateHeader("a", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 2
    }

    it("should add a value to a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.addDateHeader("a", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 2
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()

      wrappedResponse.removeHeader("a")
      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.addDateHeader("a", now)
      wrappedResponse.removeHeader("a")
      wrappedResponse.addDateHeader("a", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }
  }

  describe("appendHeader") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "a")
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "a")
    }

    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH and a quality is passed") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "a", 0.5)
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY and a quality is passed") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.appendHeader("a", "a", 0.5)
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("b", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("a", "b")

      wrappedResponse.getHeaders("a") should contain only "b"
    }

    it("should append a value if the header already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("a", "a")
      wrappedResponse.appendHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") should (include("a") and include("b"))
    }

    it("should append a value to the end of the last value if the header already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")
      wrappedResponse.appendHeader("a", "c")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 2
      originalResponse.getHeaders("a") should contain("b,c")
    }

    it("should add a value to a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("a", "a")
      wrappedResponse.appendHeader("A", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") should (include("a") and include("b"))
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.appendHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("a", "a")
      wrappedResponse.removeHeader("a")
      wrappedResponse.appendHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header with a quality when provided") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.appendHeader("b", "b", 0.5)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b;q=0.5"
    }
  }

  describe("replaceHeader") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.replaceHeader("a", "a")
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.replaceHeader("a", "a")
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("b", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("a", "b")

      wrappedResponse.getHeaders("a") should contain only "b"
    }

    it("should treat preceding interactions as unwritable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("a", "b")
    }

    it("should overwrite the value if a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("a", "a")
      wrappedResponse.replaceHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "b"
    }

    it("should overwrite the value of a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("a", "a")
      wrappedResponse.replaceHeader("A", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "b"
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.replaceHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("a", "a")
      wrappedResponse.removeHeader("a")
      wrappedResponse.replaceHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header with a quality when provided") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.replaceHeader("b", "b", 0.5)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b;q=0.5"
    }
  }

  describe("getHeadersList") {
    it("should not return any headers added by preceding interactions") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      wrappedResponse.getHeadersList("a") should be('empty)
    }

    it("should return headers added by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "b")

      wrappedResponse.getHeadersList("a") should contain only("a", "b")
    }

    it("should not return any headers removed by succeeding interactions") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeadersList("a") should be('empty)
    }

    it("should return header values with the casing they were added with") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.addHeader("a", "B")

      wrappedResponse.getHeadersList("a") should contain only("a", "B")
    }
  }

  describe("setHeader") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.setHeader("a", "a")
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.setHeader("a", "a")
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("b", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "b"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.setHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "b")

      wrappedResponse.getHeaders("a") should contain only "b"
    }

    it("should treat preceding interactions as unwritable, add a new header if it does not exist") {
      originalResponse.setHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("a", "b")
    }

    it("should overwrite the value if a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "a")
      wrappedResponse.setHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "b"
    }

    it("should overwrite the value of a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "a")
      wrappedResponse.setHeader("A", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "b"
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.setHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setHeader("a", "a")
      wrappedResponse.removeHeader("a")
      wrappedResponse.setHeader("a", "b")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "b"
    }
  }

  describe("setIntHeader") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.setIntHeader("a", 1)
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.setIntHeader("a", 1)
    }

    it("should add a new header if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("b", 1)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("b") shouldEqual "1"
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.setIntHeader("a", 1)

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("a", 2)

      wrappedResponse.getHeaders("a") should contain only "2"
    }

    it("should treat preceding interactions as unwritable, add a new header if it does not exist") {
      originalResponse.setIntHeader("a", 1)

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only("1", "2")
    }

    it("should overwrite the value if a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("a", 1)
      wrappedResponse.setIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "2"
    }

    it("should overwrite the value of a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("a", 1)
      wrappedResponse.setIntHeader("A", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "2"
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.setIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "2"
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.setIntHeader("a", 1)
      wrappedResponse.removeHeader("a")
      wrappedResponse.setIntHeader("a", 2)
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldEqual "2"
    }
  }

  describe("setDateHeader") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.setDateHeader("a", System.currentTimeMillis())
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.setDateHeader("a", System.currentTimeMillis())
    }

    it("should add a new header, in a RFC2616 compliant format, if it does not exist") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      val now = System.currentTimeMillis()

      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.commitToResponse()

      DateUtils.parseDate(originalResponse.getHeader("a")) should not be null
    }

    it("should treat preceding interactions as unreadable, add a new header if it does not exist") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      val now = System.currentTimeMillis()

      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 2
    }

    it("should overwrite the value of a header if it already exists") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.setDateHeader("a", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }

    it("should overwrite the value of a header if it already exists, regardless of casing") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.setDateHeader("a", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }

    it("should add a header even if it was previously deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()

      wrappedResponse.removeHeader("a")
      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }

    it("should add a header even if it was previously added then deleted") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      val now = System.currentTimeMillis()
      val later = now + 1000

      wrappedResponse.setDateHeader("a", now)
      wrappedResponse.removeHeader("a")
      wrappedResponse.setDateHeader("a", later)
      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a").size() shouldEqual 1
    }
  }

  describe("removeHeader") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.removeHeader("a")
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.READONLY, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.removeHeader("a")
    }

    it("should not remove a header from the wrapped response") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.removeHeader("a")
      wrappedResponse.commitToResponse()

      originalResponse.getHeader("a") shouldBe "a"
    }

    it("should remove a header that has been added") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("a")

      wrappedResponse.getHeader("a") shouldBe null
    }

    it("should remove a header in a case-insensitive way") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)

      wrappedResponse.addHeader("a", "a")
      wrappedResponse.removeHeader("A")

      wrappedResponse.getHeader("a") shouldBe null
    }
  }

  describe("getOutputStreamAsInputStream") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.getOutputStreamAsInputStream
    }

    it("should return an input stream containing the readable contents of the output stream") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.READONLY)

      val body = "test body"
      wrappedResponse.getOutputStream.print(body)

      val wrappedBody = wrappedResponse.getOutputStreamAsInputStream

      Source.fromInputStream(wrappedBody).mkString shouldEqual body
    }
  }

  describe("setOutput") {
    it("should throw an IllegalStateException if the header mode is set to PASSTHROUGH") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)

      an[IllegalStateException] should be thrownBy wrappedResponse.setOutput(new ByteArrayInputStream("test body".getBytes()))
    }

    it("should throw an IllegalStateException if the header mode is set to READONLY") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.READONLY)

      an[IllegalStateException] should be thrownBy wrappedResponse.setOutput(new ByteArrayInputStream("test body".getBytes()))
    }

    it("should set the output") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.READONLY)

      val body = "test body"
      wrappedResponse.setOutput(new ByteArrayInputStream(body.getBytes))

      val wrappedBody = wrappedResponse.getOutputStreamAsInputStream

      Source.fromInputStream(wrappedBody).mkString shouldEqual body
    }

    it("should set the output, clearing any previous output") {
      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.PASSTHROUGH, ResponseMode.READONLY)
      wrappedResponse.getOutputStream.print("foo")

      val body = "test body"
      wrappedResponse.setOutput(new ByteArrayInputStream(body.getBytes))

      val wrappedBody = wrappedResponse.getOutputStreamAsInputStream

      Source.fromInputStream(wrappedBody).mkString shouldEqual body
    }
  }

  describe("getWriter") {
    pending
  }

  describe("getOutputStream") {
    pending
  }

  describe("commitToResponse") {
    Seq(
      (ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH),
      (ResponseMode.PASSTHROUGH, ResponseMode.READONLY),
      (ResponseMode.READONLY, ResponseMode.PASSTHROUGH),
      (ResponseMode.READONLY, ResponseMode.READONLY)
    ) foreach { case (headerMode, bodyMode) =>
      it(s"should throw an IllegalStateException if the header mode is set to ${headerMode.name()} and body mode is set to ${bodyMode.name()}") {
        val wrappedResponse = new HttpServletResponseWrapper(originalResponse, headerMode, bodyMode)

        an[IllegalStateException] should be thrownBy wrappedResponse.commitToResponse()
      }
    }

    it("should not alter pre-existing headers in the wrapped response") {
      originalResponse.addHeader("a", "a")

      val wrappedResponse = new HttpServletResponseWrapper(originalResponse, ResponseMode.MUTABLE, ResponseMode.PASSTHROUGH)
      wrappedResponse.addHeader("b", "b")

      wrappedResponse.commitToResponse()

      originalResponse.getHeaders("a") should contain only "a"
      originalResponse.getHeaders("b") should contain only "b"
    }
  }
}
