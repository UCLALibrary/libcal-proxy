
package edu.ucla.library.libcal.handlers;

import static edu.ucla.library.libcal.MediaType.APPLICATION_JSON;
import static edu.ucla.library.libcal.MediaType.TEXT_PLAIN;
import static info.freelibrary.util.Constants.EMPTY;
import static info.freelibrary.util.Constants.SLASH;

import info.freelibrary.util.HTTP;

import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.JsonKeys;
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
@SuppressWarnings("PMD.UnusedLocalVariable")
public class ProxyHandler implements Handler<RoutingContext> {

    /**
     * The handler's logger.
     */
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
        final String path = aContext.request().path();
        final String receivedQuery = path.replaceAll("/libcal/", "");
        // final String receivedQuery = path.substring(path.indexOf("libcal/") + 1);

        if (receivedQuery == null || receivedQuery.equals(EMPTY)) {
            response.setStatusCode(HTTP.BAD_REQUEST).putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN.toString())
                    .end(LOGGER.getMessage(MessageCodes.LCP_006));
        } else {
            LibCalProxyService.create(myVertx, myConfig).compose(proxy -> {
                //final MessageConsumer<?> myProxy = new ServiceBinder(myVertx).setAddress(LibCalProxyService.ADDRESS)
                  //      .register(LibCalProxyService.class, proxy);
                myApiProxy = LibCalProxyService.createProxy(myVertx);
                return OAuthTokenService.create(myVertx, myConfig).compose(tokenService -> {
                    //final MessageConsumer<?> myToken = new ServiceBinder(myVertx).setAddress(OAuthTokenService.ADDRESS)
                      //      .register(OAuthTokenService.class, tokenService);
                    myTokenProxy = OAuthTokenService.createProxy(myVertx);
                    return myTokenProxy.getBearerToken().compose(token -> {
                        return myApiProxy.getLibCalOutput(token, SLASH.concat(receivedQuery)).onSuccess(apiOutput -> {
                            response.setStatusCode(HTTP.OK).putHeader(HttpHeaders.CONTENT_TYPE,
                                    APPLICATION_JSON.toString());
                            response.end(apiOutput);
                        }).onFailure(failure -> {
                            returnError(response, HTTP.INTERNAL_SERVER_ERROR, failure.getMessage());
                        });
                    }).onFailure(failure -> {
                        returnError(response, HTTP.INTERNAL_SERVER_ERROR, failure.getMessage());
                    });
                }).onFailure(failure -> {
                    returnError(response, HTTP.INTERNAL_SERVER_ERROR, failure.getMessage());
                });
            }).onFailure(failure -> {
                returnError(response, HTTP.INTERNAL_SERVER_ERROR, failure.getMessage());
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

    /**
     * Return an error message/response code to the requester.
     *
     * @param aResponse A HTTP response
     * @param aStatusCode A HTTP response code
     * @param aError An exception message
     */
    private void returnError(final HttpServerResponse aResponse, final int aStatusCode, final String aError) {
        final JsonObject errorBody = new JsonObject().put(JsonKeys.ERROR,
                LOGGER.getMessage(MessageCodes.LCP_007, aError.replaceAll(Constants.EOL_REGEX, Constants.BR_TAG)));

        aResponse.setStatusCode(aStatusCode);
        aResponse.setStatusMessage(aError.replaceAll(Constants.EOL_REGEX, EMPTY));
        aResponse.putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
        aResponse.end(errorBody.encodePrettily());
    }
}
