
package edu.ucla.library.libcal.handlers;

import static edu.ucla.library.libcal.MediaType.APPLICATION_JSON;

import info.freelibrary.util.HTTP;

import edu.ucla.library.libcal.Config;
// activate after SERV-442 merge
//import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.JsonKeys;
// activate after SERV-442 merge
//import eduedu.ucla.library.libcal.services.LibCalProxyService;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that processes status information requests.
 */
public class ProxyHandler implements Handler<RoutingContext> {

    /**
     * The handler's copy of the Vert.x instance.
     */
    private final Vertx myVertx;

    /**
     * A service for LibCal OAuth and API calls
     */
    private final LibCalProxyService myProxyService;

    /**
     * Creates a handler that returns a status response.
     *
     * @param aVertx A Vert.x instance
     */
    public ProxyHandler(final Vertx aVertx) {
        myVertx = aVertx;
	myProxyService = LibCalProxyService.createProxy(myVertx);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final String receivedApp = aContext.pathParam("theApp");
	final String receivedQuery = aContext.pathParam("theQuery");

        if (receivedApp == null || receivedApp.length() == 0) {
            response.setStatusCode(HTTP.BAD_REQUEST).end("missing app param"); //add message bundle message here
            return;
	} else if (receivedQuery == null || receivedQuery.length() ==0) {
            response.setStatusCode(HTTP.BAD_REQUEST).end("missing query param"); //add message bundle message here
            return;
	} else {
            response.setStatusCode(HTTP.OK).putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
	}
    }

    /**
     * Gets the Vert.x instance associated with this handler.
     *
     * @return The Vert.x instance associated with this handler
     */
    public Vertx getVertx() {
        return myVertx;
    }
}
