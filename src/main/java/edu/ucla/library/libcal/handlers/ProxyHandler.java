
package edu.ucla.library.libcal.handlers;

import static edu.ucla.library.libcal.MediaType.APPLICATION_JSON;

import info.freelibrary.util.HTTP;

import edu.ucla.library.libcal.JsonKeys;

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
     * Creates a handler that returns a status response.
     *
     * @param aVertx A Vert.x instance
     */
    public ProxyHandler(final Vertx aVertx) {
        myVertx = aVertx;
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final String receivedApp = aContext.pathParam("theApp");
	final String receivedQuery = aContext.pathParam("theQuery");

	response.setStatusCode(HTTP.OK).putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
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
