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
package org.openrepose.commons.utils.servlet.http;

import org.openrepose.commons.utils.http.HttpDate;
import org.openrepose.commons.utils.http.header.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public final class HeaderValuesImpl implements HeaderValues {

    private static final String HEADERS_PREFIX = "repose.headers.";
    private final Map<HeaderName, List<HeaderValue>> headers;


    private HeaderValuesImpl(HttpServletRequest request, HeaderContainer container) {
        this.headers = initHeaders(request, container);
        cloneHeaders(container);
    }

    public static HeaderValues extract(HttpServletRequest request) {
        return new HeaderValuesImpl(request, new RequestHeaderContainer(request));
    }

    public static HeaderValues extract(HttpServletRequest request, HttpServletResponse response) {
        return new HeaderValuesImpl(request, new ResponseHeaderContainer(response));
    }

    static <T> T fromMap(Map<HeaderName, List<T>> headers, String headerName) {
        final List<T> headerValues = headers.get(HeaderName.wrap(headerName));

        return (headerValues != null && !headerValues.isEmpty()) ? headerValues.get(0) : null;
    }

    private Map<HeaderName, List<HeaderValue>> initHeaders(HttpServletRequest request, HeaderContainer container) {
        Map<HeaderName, List<HeaderValue>> currentHeaderMap = (Map<HeaderName, List<HeaderValue>>) request
                .getAttribute(HEADERS_PREFIX + container.getContainerType().name());

        if (currentHeaderMap == null) {
            currentHeaderMap = new HashMap<HeaderName, List<HeaderValue>>();
            request.setAttribute(HEADERS_PREFIX + container.getContainerType().name(), currentHeaderMap);
        }

        return currentHeaderMap;
    }

    private void cloneHeaders(HeaderContainer request) {

        final Map<HeaderName, List<HeaderValue>> headerMap = new HashMap<HeaderName, List<HeaderValue>>();
        final List<HeaderName> headerNames = request.getHeaderNames();

        for (HeaderName headerName : headerNames) {

            final List<HeaderValue> headerValues = request.getHeaderValues(headerName.getName());
            headerMap.put(headerName, headerValues);
        }

        headers.clear();
        headers.putAll(headerMap);
    }

    private List<HeaderValue> parseHeaderValues(HeaderName headerName, String value) {
        HeaderFieldParser parser = new HeaderFieldParser(value, headerName.getName());

        return parser.parse();
    }

    @Override
    public void addHeader(String name, String value) {
        final HeaderName wrappedName = HeaderName.wrap(name);

        List<HeaderValue> headerValues = headers.get(wrappedName);

        if (headerValues == null) {
            headerValues = new LinkedList<HeaderValue>();
        }

        headerValues.add(new HeaderValueImpl(value));

        headers.put(wrappedName, headerValues);
    }

    @Override
    public void replaceHeader(String name, String value) {
        final List<HeaderValue> headerValues = new LinkedList<HeaderValue>();

        HeaderName wrappedName = HeaderName.wrap(name);

        headerValues.addAll(parseHeaderValues(wrappedName, value));

        headers.put(wrappedName, headerValues);
    }

    @Override
    public void removeHeader(String name) {
        headers.remove(HeaderName.wrap(name));
    }

    @Override
    public void clearHeaders() {
        headers.clear();
    }

    @Override
    public String getHeader(String name) {
        HeaderValue value = fromMap(headers, name);
        return value != null ? value.toString() : null;
    }

    @Override
    public HeaderValue getHeaderValue(String name) {
        return fromMap(headers, name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<HeaderName> headerNamesWrapped = headers.keySet();
        Set<String> headerNamesAsStrings = new HashSet<String>();

        for (HeaderName wrappedName : headerNamesWrapped) {
            headerNamesAsStrings.add(wrappedName.getName());
        }

        return Collections.enumeration(headerNamesAsStrings);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        final List<HeaderValue> headerValues = headers.get(HeaderName.wrap(name));
        final List<String> values = new LinkedList<String>();

        if (headerValues != null) {
            for (HeaderValue value : headerValues) {
                values.add(value.toString());
            }
        }

        return Collections.enumeration(values);
    }

    @Override
    public List<HeaderValue> getPreferredHeaderValues(String name, HeaderValue defaultValue) {
        List<HeaderValue> headerValues = headers.get(HeaderName.wrap(name));

        QualityFactorHeaderChooser chooser = new QualityFactorHeaderChooser<HeaderValue>();
        List<HeaderValue> values = chooser.choosePreferredHeaderValues(headerValues);

        if (values.isEmpty() && defaultValue != null) {
            values.add(defaultValue);
        }

        return values;

    }

    @Override
    public List<HeaderValue> getPreferredHeaders(String name, HeaderValue defaultValue) {
        List<HeaderValue> headerValues = headers.get(HeaderName.wrap(name));

        if (headerValues == null || headerValues.isEmpty()) {
            headerValues = new ArrayList<HeaderValue>();
            if (defaultValue != null) {
                headerValues.add(defaultValue);
            }
            return headerValues;
        }

        Map<Double, List<HeaderValue>> groupedHeaderValues = new LinkedHashMap<Double, List<HeaderValue>>();

        for (HeaderValue value : headerValues) {

            if (!groupedHeaderValues.keySet().contains(value.getQualityFactor())) {
                groupedHeaderValues.put(value.getQualityFactor(), new LinkedList<HeaderValue>());
            }

            groupedHeaderValues.get(value.getQualityFactor()).add(value);
        }

        headerValues.clear();

        List<Double> qualities = new ArrayList<Double>(groupedHeaderValues.keySet());
        java.util.Collections.sort(qualities);
        java.util.Collections.reverse(qualities);

        for (Double quality : qualities) {
            headerValues.addAll(groupedHeaderValues.get(quality));
        }

        if (headerValues.isEmpty() && defaultValue != null) {
            headerValues.add(defaultValue);
        }

        return headerValues;
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(HeaderName.wrap(name));
    }

    @Override
    public void addDateHeader(String name, long value) {
        HeaderName wrappedName = HeaderName.wrap(name);

        List<HeaderValue> headerValues = headers.get(wrappedName);

        if (headerValues == null) {
            headerValues = new LinkedList<HeaderValue>();
        }

        HttpDate date = new HttpDate(new Date(value));
        headerValues.add(new HeaderValueImpl(date.toRFC1123()));

        headers.put(wrappedName, headerValues);
    }

    @Override
    public void replaceDateHeader(String name, long value) {
        headers.remove(HeaderName.wrap(name));
        addDateHeader(name, value);
    }

    @Override
    public List<HeaderValue> getHeaderValues(String name) {
        return headers.get(HeaderName.wrap(name));
    }
}
