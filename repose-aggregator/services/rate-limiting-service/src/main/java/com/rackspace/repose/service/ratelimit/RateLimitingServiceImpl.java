package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.RateLimitList;
import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.cache.CachedRateLimit;
import com.rackspace.repose.service.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.config.*;
import com.rackspace.repose.service.ratelimit.exception.OverLimitException;
import com.rackspace.repose.service.ratelimit.util.StringUtilities;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RateLimitingServiceImpl implements RateLimitingService {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingServiceImpl.class);
    private final RateLimitCache cache;
    private final RateLimiter rateLimiter;
    private final RateLimitingConfigHelper helper;
    private final boolean useCaptureGroups;

    public RateLimitingServiceImpl(RateLimitCache cache, RateLimitingConfiguration rateLimitingConfiguration) {

        if (rateLimitingConfiguration == null) {
            throw new IllegalArgumentException("Rate limiting configuration must not be null.");
        }

        this.cache = cache;
        this.rateLimiter = new RateLimiter(cache);
        this.helper = new RateLimitingConfigHelper(rateLimitingConfiguration);
        useCaptureGroups = rateLimitingConfiguration.isUseCaptureGroups();
    }

    @Override
    public RateLimitList queryLimits(String user, List<String> groups) {

        if (StringUtilities.isBlank(user)) {
            throw new IllegalArgumentException("User required when querying rate limits.");
        }

        final Map<String, CachedRateLimit> cachedLimits = cache.getUserRateLimits(user);
        final ConfiguredLimitGroup configuredLimitGroup = helper.getConfiguredGroupByRole(groups);
        final RateLimitListBuilder limitsBuilder = new RateLimitListBuilder(cachedLimits, configuredLimitGroup);

        return limitsBuilder.toRateLimitList();
    }

    @Override
    public void trackLimits(String user, List<String> groups, String uri, String queryString, String httpMethod, int datastoreWarnLimit) throws OverLimitException {

        if (StringUtilities.isBlank(user)) {
            throw new IllegalArgumentException("User required when tracking rate limits.");
        }

        final ConfiguredLimitGroup configuredLimitGroup = helper.getConfiguredGroupByRole(groups);
        final List< Pair<String, ConfiguredRatelimit> > matchingConfiguredLimits = new ArrayList< Pair<String, ConfiguredRatelimit> >();
        TimeUnit largestUnit = null;

        // Go through all of the configured limits for this group
        for (ConfiguredRatelimit rateLimit : configuredLimitGroup.getLimit()) {
            Matcher uriMatcher;
            if (rateLimit instanceof ConfiguredRateLimitWrapper) {
                uriMatcher = ((ConfiguredRateLimitWrapper) rateLimit).getRegexPattern().matcher(uri);
            } else {
                LOG.error("Unable to locate pre-built regular expression pattern in for limit group.  This state is not valid. "
                        + "In order to continue operation, rate limiting will compile patterns dynamically.");
                uriMatcher = Pattern.compile(rateLimit.getUriRegex()).matcher(uri);
            }

            // Did we find a limit that matches the incoming uri and http method?
            if (uriMatcher.matches() && httpMethodMatches(rateLimit.getHttpMethods(), httpMethod) && queryStringMatches(rateLimit.getQueryStringRegex(), queryString)) {
                matchingConfiguredLimits.add(Pair.of(LimitKey.getLimitKey(configuredLimitGroup.getId(),
                        rateLimit.getId(), uriMatcher, useCaptureGroups), rateLimit));

                if (largestUnit == null || rateLimit.getUnit().compareTo(largestUnit) > 0) {
                    largestUnit = rateLimit.getUnit();
                }
            }
        }
        if (matchingConfiguredLimits.size() > 0) {
            rateLimiter.handleRateLimit(user, matchingConfiguredLimits, largestUnit, datastoreWarnLimit);
        }
    }

    private boolean httpMethodMatches(List<HttpMethod> configMethods, String requestMethod) {
        return configMethods.contains(HttpMethod.ALL) || configMethods.contains(HttpMethod.valueOf(requestMethod.toUpperCase()));
    }

    private boolean queryStringMatches(String configuredQueryStringRegex, String requestQueryString) {
        /* Check pre-conditions */
        if (configuredQueryStringRegex == null || configuredQueryStringRegex.length() == 0) { return true; }
        else if (requestQueryString == null) { return false; }

        /* The following splits should be safe since '&' is reserved as a delimiter in a query string according to
         * RFC 3986 */
        String[] configuredParameterRegexes = configuredQueryStringRegex.split("&");
        String[] requestParameters = requestQueryString.split("&");

        for (String parameterRegex : configuredParameterRegexes) {
            boolean matchFound = false;
            Pattern pattern = Pattern.compile(parameterRegex);

            for (String requestParameter : requestParameters) {
                if (pattern.matcher(decodeQueryString(requestParameter)).matches()) {
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) { return false; }
        }

        return true;
    }

    private String decodeQueryString(String queryString) {
        String processedQueryString = queryString;

        try {
            processedQueryString = URLDecoder.decode(processedQueryString.replace("+", "%2B"), "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            /* Since we've hardcoded the UTF-8 encoding, this should never occur. */
            LOG.error("RateLimitingService.decodeQueryString - Unsupported Encoding", uee);
        }

        return processedQueryString;
    }
}
