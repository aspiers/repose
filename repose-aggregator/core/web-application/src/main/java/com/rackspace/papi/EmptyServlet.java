package com.rackspace.papi;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.service.context.impl.PowerApiContextManager;
import com.rackspace.papi.servlet.InitParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class EmptyServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(EmptyServlet.class);
    private boolean initialized = false;

    private boolean isRequestFilterChainComplete(HttpServletRequest req) {
        return (Boolean) req.getAttribute("filterChainAvailableForRequest");
    }

    private boolean isPowerApiContextManagerIntiliazed() {
        if (initialized) {
            return true;
        }

        PowerApiContextManager manager = (PowerApiContextManager) getServletContext().getAttribute("powerApiContextManager");

        if (manager == null || !manager.isContextInitialized()) {
            return false;
        }

        initialized = true;

        return initialized;
    }

    @Override
    public void init() throws ServletException {
        if (System.getProperty(InitParameter.REPOSE_CLUSTER_ID.getParameterName()) == null) {
            throw new UnavailableException("EmptyServlet.init() : repose-cluster-id not provided");
        } else if (System.getProperty(InitParameter.REPOSE_NODE_ID.getParameterName()) == null) {
            throw new UnavailableException("EmptyServlet.init() : repose-node-id not provided");
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isPowerApiContextManagerIntiliazed() || !isRequestFilterChainComplete(req)) {
            LOG.debug("Filter chain is not available to process request.");
            resp.sendError(HttpStatusCode.SERVICE_UNAVAIL.intValue(), "Filter chain is not available to process request");
        }
    }
}
