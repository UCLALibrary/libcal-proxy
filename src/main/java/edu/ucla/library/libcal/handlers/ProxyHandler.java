
package edu.ucla.library.libcal.handlers;

import static edu.ucla.library.libcal.MediaType.APPLICATION_JSON;

import info.freelibrary.util.HTTP;

import edu.ucla.library.libcal.Constants;
import eduedu.ucla.library.libcal.services.LibCalProxyService;
import eduedu.ucla.library.libcal.services.OAuthTokenService;

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
     * The handler's copy of the application config.
     */
    private final JsonObject myConfig;

    /**
     * A service for LibCal API calls
     */
    private final LibCalProxyService myAPIProxy;

    /**
     * A service for LibCal OAuth calls
     */
    private final OAuthTokenService myTokenProxy;

    /**
     * Creates a handler that returns a status response.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A configuration
     */
    public ProxyHandler(final Vertx aVertx, final JsonObject aConfig) {
        myVertx = aVertx;
        myConfig = aConfig;
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final String receivedQuery = aContext.pathParam(Constants.QUERY_PARAM);

        if (receivedQuery == null || receivedQuery.equals(Constants.EMPTY)) {
            response.setStatusCode(HTTP.BAD_REQUEST).end(LOGGER.getMessage(MessageCodes.LCP_005));
            return;
        } else {
            response.setStatusCode(HTTP.OK).putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
            OAuthTokenService.create(myVertx, myConfig)
            .onSuccess(service -> {
                final MessageConsumer<?> myToken = new ServiceBinder(myVertx).setAddress(OAuthTokenService.ADDRESS)
                      .register(OAuthTokenService.class, service);
                myTokenProxy = OAuthTokenService.createProxy(myVertx);
                myTokenProxy.getBearerToken().compose(token -> {
                    return LibCalProxyService.create(aVertx, config);
                }).onSuccess(apiProxy -> {
                    final MessageConsumer<?> myAPI = new ServiceBinder(myVertx).setAddress(LibCalProxyService.ADDRESS)
                          .register(LibCalProxyService.class, apiProxy);
                    myAPIProxy = LibCalProxyService.createProxy(myVertx);
                    myServiceProxy.getLibCalOutput(token, Constants.SLASH.concat(receivedQuery))
                    .onSuccess(apiOutput -> {
                        response.setStatusCode(HTTP.OK).putHeader(
                                 HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
                        response.end(apiOutput);
                    }).onFailure();
                }).onFailure();
            }).onFailure();
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
