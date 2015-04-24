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
package org.openrepose.filters.uristripper;

import org.jvnet.jaxb2_commons.lang.StringUtils;
import org.openrepose.commons.utils.StringUriUtilities;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UriStripperHandler extends AbstractFilterLogicHandler {

    public static final String URI_DELIMITER = "/";
    public static final String QUERY_PARAM_INDICATOR = "?";
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(UriStripperHandler.class);
    int stripId;
    boolean rewriteLocation;
    String prevToken, nextToken, token;
    StringBuilder preText, postText;
    String locationHeader;

    public UriStripperHandler(int stripId, boolean rewriteLocation) {

        this.rewriteLocation = rewriteLocation;
        this.stripId = stripId;

        this.preText = new StringBuilder();
        this.postText = new StringBuilder();

    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

        final FilterDirector filterDirector = new FilterDirectorImpl();

        filterDirector.setFilterAction(FilterAction.PASS);

        List<String> uriList = getUriAsDelimitedList(request.getRequestURI());

        if (uriList.size() > stripId) {
            //Preserve the tokens before and after the stripped token
            if (uriList.size() > stripId + 1) {
                nextToken = uriList.get(stripId + 1);
            }
            // stripId=0 means we're stripping out the first token in the resource path. No need to grab that info
            if (stripId != 0 && 1 < uriList.size()) {
                prevToken = uriList.get(stripId - 1);
            }
            // strip out configured item
            token = uriList.remove(stripId);
            if (rewriteLocation) {
                filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
            }
        }

        filterDirector.setRequestUri(StringUriUtilities.formatUri(StringUtils.join(uriList.iterator(), URI_DELIMITER)));

        return filterDirector;
    }

    // Mostly to deal with the location header if it is present
    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {

        FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setResponseStatusCode(response.getStatus());
        filterDirector.setFilterAction(FilterAction.PASS);

        if (StringUtilities.isNotBlank(response.getHeader(CommonHttpHeader.LOCATION.toString())) && (StringUtilities.isNotBlank(prevToken)
                || StringUtilities.isNotBlank(nextToken))) {

            locationHeader = response.getHeader(CommonHttpHeader.LOCATION.toString());

            //Need to check and see if it contains the three token pattern already in the location header
            if (locationHeader.contains(prevToken + URI_DELIMITER + token + URI_DELIMITER + nextToken)) {
                LOG.debug("Stripped token already present in Location Header");
                return filterDirector;
            }

            try {
                extractPreAndPostTexts(locationHeader);
            } catch (URISyntaxException ex) {
                LOG.warn("Unable to parse Location header. Location header is malformed URI", ex.getMessage(), ex);
                return filterDirector;
            }

            List<String> uri = getUriAsDelimitedList(locationHeader);

            // now do logic to put back the prev and post tokens

            if (uri.contains(prevToken)) {

                //Case 1 both prev and next are present and they are next to each other
                //Case 2 both prev and next are present, but they are not next to each other
                uri.add(uri.indexOf(prevToken) + 1, token);
            } else if (uri.contains(nextToken)) {
                //Case 4 next text is present, prev text is not present

                uri.add(uri.indexOf(nextToken), token);
            }

            //Rebuild location header
            StringBuilder newLoc = new StringBuilder(preText).append(StringUriUtilities.formatUri(StringUtils.join(uri.iterator(), URI_DELIMITER)));

            if (postText.length() != 0) {
                newLoc.append(QUERY_PARAM_INDICATOR).append(postText);
            }
            filterDirector.responseHeaderManager().putHeader(CommonHttpHeader.LOCATION.toString(), newLoc.toString());
        }

        return filterDirector;

    }

    private List<String> getUriAsDelimitedList(String uri) {

        uri = StringUriUtilities.formatUriNoLead(uri);

        List<String> uriList = new ArrayList(Arrays.asList(uri.split(URI_DELIMITER)));

        return uriList;
    }


    private void extractPreAndPostTexts(String locationUrl) throws URISyntaxException {

        URI uri = new URI(locationUrl);
        locationHeader = uri.getPath();
        if (uri.getScheme() != null) {
            extractPreText(uri);
        }
        postText = new StringBuilder(StringUtilities.getNonBlankValue(uri.getQuery(), ""));

    }

    private void extractPreText(URI uri) {

        preText = preText.append(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() != -1) {
            preText.append(":").append(uri.getPort());
        }

    }

}
