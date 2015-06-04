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

import java.util
import javax.servlet.http.HttpServletRequest

import org.apache.http.client.utils.DateUtils

import scala.collection.JavaConverters._
import scala.collection.immutable.{TreeMap, TreeSet}

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 5/27/15
 * Time: 10:25 AM
 */
class HttpServletRequestWrapper(originalRequest: HttpServletRequest)
  extends javax.servlet.http.HttpServletRequestWrapper(originalRequest)
  with HeaderInteractor {

  private var headerMap: Map[String, List[String]] = new TreeMap[String, List[String]]()(CaseInsensitiveStringOrdering)
  private var removedHeaders: Set[String] = new TreeSet[String]()(CaseInsensitiveStringOrdering)

  object CaseInsensitiveStringOrdering extends Ordering[String] {
    override def compare(x: String, y: String): Int = x compareToIgnoreCase y
  }

  def getHeaderNamesScala: Set[String] =
    super.getHeaderNames.asScala.toSet.filterNot(removedHeaders.contains) ++ headerMap.keySet

  override def getHeaderNames: util.Enumeration[String] = getHeaderNamesScala.toIterator.asJavaEnumeration

  override def getHeaderNamesList: util.List[String] = getHeaderNamesScala.toList.asJava

  override def getIntHeader(headerName: String): Int = Option(getHeader(headerName)).map(_.toInt).getOrElse(-1)

  def getHeadersScala(headerName: String): List[String] =
    if (removedHeaders.contains(headerName)) Nil
    else headerMap.getOrElse(headerName, super.getHeaders(headerName).asScala.toList)

  override def getHeaders(headerName: String): util.Enumeration[String] = getHeadersScala(headerName).toIterator.asJavaEnumeration

  override def getDateHeader(headerName: String): Long = {
    Option(getHeader(headerName)) match {
      case Some(headerValue) =>
        Option(DateUtils.parseDate(headerValue)) match {
          case Some(parsedDate) => parsedDate.getTime
          case None => throw new IllegalArgumentException("Header value could not be converted to a date")
        }
      case None => -1
    }
  }

  override def getHeader(headerName: String): String = getHeadersScala(headerName).headOption.orNull

  override def getHeadersList(headerName: String): util.List[String] = getHeadersScala(headerName).asJava

  override def addHeader(headerName: String, headerValue: String): Unit = {
    // Getting the current values of this header must be done before we remove from the removedHeaders collection
    // because the current header values are partially determined by the contents of the removedHeaders collection.
    val existingHeaders: List[String] = getHeadersScala(headerName)
    if (removedHeaders.contains(headerName)) {
      removedHeaders = removedHeaders.filterNot(_.equalsIgnoreCase(headerName))
    }
    headerMap = headerMap + (headerName -> (existingHeaders :+ headerValue))
  }

  override def addHeader(headerName: String, headerValue: String, quality: Double): Unit =
    addHeader(headerName, headerValue + ";q=" + quality)

  override def appendHeader(headerName: String, headerValue: String): Unit = {
    val existingHeaders: List[String] = getHeadersScala(headerName)
    existingHeaders.headOption match {
      case Some(value) =>
        val newHeadValue: String = value + "," + headerValue
        headerMap = headerMap + (headerName -> (newHeadValue +: existingHeaders.tail))
      case None => addHeader(headerName, headerValue)
    }
  }

  override def appendHeader(headerName: String, headerValue: String, quality: Double): Unit =
    appendHeader(headerName, headerValue + ";q=" + quality)

  override def removeHeader(headerName: String): Unit = {
    removedHeaders = removedHeaders + headerName
    headerMap = headerMap.filterKeys(!_.equalsIgnoreCase(headerName))
  }

  def getPreferredHeader(headerName: String, getFun: String => List[String]): String = {
    def parseQuality(headerValue: String): Double = {
      //todo: what if the parameter value is not a number? Try and default to 1.0?
      headerValue.split(";").tail.find(param => "q".equalsIgnoreCase(param.split("=")(0).trim))
        .map(_.split("=", 2)(1).toDouble).getOrElse(1.0)
    }

    getFun(headerName) match {
      case Nil => null
      case nonEmptyList => nonEmptyList.maxBy(parseQuality).split(";")(0)
    }
  }

  override def getPreferredHeader(headerName: String): String = {
    getPreferredHeader(headerName, getHeadersScala)
  }

  override def getPreferredSplittableHeader(headerName: String): String = {
    getPreferredHeader(headerName, getSplittableHeaderScala)
  }

  override def replaceHeader(headerName: String, headerValue: String): Unit = {
    headerMap = headerMap + (headerName -> List(headerValue))
    removedHeaders = removedHeaders.filterNot(_.equalsIgnoreCase(headerName))
  }

  override def replaceHeader(headerName: String, headerValue: String, quality: Double): Unit = replaceHeader(headerName, headerValue + ";q=" + quality)

  def getSplittableHeaderScala(headerName: String): List[String] = getHeadersScala(headerName).foldLeft(List[String]())((list, s) => list ++ s.split(","))

  override def getSplittableHeader(headerName: String): util.List[String] = getSplittableHeaderScala(headerName).asJava
}
