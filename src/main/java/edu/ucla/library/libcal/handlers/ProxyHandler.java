
package edu.ucla.library.libcal.handlers;

import static edu.ucla.library.libcal.MediaType.APPLICATION_JSON;
import static edu.ucla.library.libcal.MediaType.TEXT_PLAIN;

import info.freelibrary.util.HTTP;

import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.MessageCodes;
import edu.ucla.library.libcal.services.LibCalProxyService;
import edu.ucla.library.libcal.services.OAuthTokenService;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * A handler that processes status information requests.
 */
public class ProxyHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHandler.class, MessageCodes.BUNDLE);

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
    private LibCalProxyService myApiProxy;

    /**
     * A service for LibCal OAuth calls
     */
    private OAuthTokenService myTokenProxy;

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
            response.setStatusCode(HTTP.BAD_REQUEST).end(LOGGER.getMessage(MessageCodes.LCP_006));
            return;
        } else {
            LibCalProxyService.create(myVertx, myConfig).compose(proxy -> {
                final MessageConsumer<?> myProxy = new ServiceBinder(myVertx)
                      .setAddress(LibCalProxyService.ADDRESS)
                      .register(LibCalProxyService.class, proxy);
                myApiProxy = LibCalProxyService.createProxy(myVertx);
                return OAuthTokenService.create(myVertx, myConfig).compose(tokenService -> {
                    final MessageConsumer<?> myToken = new ServiceBinder(myVertx)
                          .setAddress(OAuthTokenService.ADDRESS)
                          .register(OAuthTokenService.class, tokenService);
                    myTokenProxy = OAuthTokenService.createProxy(myVertx);
                    return myTokenProxy.getBearerToken().compose(token -> {
                        return myApiProxy.getLibCalOutput(token,
                              Constants.SLASH.concat(receivedQuery)).onSuccess(apiOutput -> {
                                  response.setStatusCode(HTTP.OK).putHeader(
                                      HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
                                  response.end(apiOutput);
                              }).onFailure(failure -> {
                                  final String statusMessage = failure.getMessage();
                                  final String errorMessage = LOGGER.getMessage(MessageCodes.LCP_007, statusMessage);

                                  LOGGER.error(errorMessage);
                                  response.setStatusCode(HTTP.INTERNAL_SERVER_ERROR);
                                  response.putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN.toString());
                                  response.end(errorMessage);
                              });
                    });
                });
            });
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
