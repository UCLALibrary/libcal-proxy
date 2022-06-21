
package edu.ucla.library.libcal.handlers;

import static edu.ucla.library.libcal.MediaType.APPLICATION_JSON;
import static info.freelibrary.util.Constants.COMMA;
import static info.freelibrary.util.Constants.EMPTY;
import static info.freelibrary.util.Constants.SLASH;

import com.github.veqryn.collect.Cidr4Trie;
import com.github.veqryn.net.Cidr4;
import com.github.veqryn.net.Ip4;

import info.freelibrary.util.HTTP;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.JsonKeys;
import edu.ucla.library.libcal.MessageCodes;
import edu.ucla.library.libcal.services.LibCalProxyService;
import edu.ucla.library.libcal.services.OAuthTokenService;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

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
     * The handler's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHandler.class, MessageCodes.BUNDLE);

    /**
     * A constant for the "?" to lead an HTTP query string.
     */
    private static final String QUESTION_MARK = "?";

    /**
     * The handler's copy of the Vert.x instance.
     */
    private final Vertx myVertx;

    /**
     * The handler's copy of the Vert.x instance.
     */
    private final JsonObject myConfig;

    /**
     * A service for LibCal API calls
     */
    private final LibCalProxyService myApiProxy;

    /**
     * A service for LibCal OAuth calls
     */
    private final OAuthTokenService myTokenProxy;

    /**
     * Creates a handler that returns a status response.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig Application config stored in JSON
     */
    public ProxyHandler(final Vertx aVertx, final JsonObject aConfig) {
        myVertx = aVertx;
        myConfig = aConfig;
        myApiProxy = LibCalProxyService.createProxy(myVertx);
        myTokenProxy = OAuthTokenService.createProxy(myVertx);
    }

    @Override
    public void handle(final RoutingContext aContext) {
        final HttpServerResponse response = aContext.response();
        final String path = aContext.request().path();
        final String originalClientIP = aContext.request().remoteAddress().hostAddress();
        final Cidr4Trie<String> allowedIPs = buildAllowedNetwork(myConfig.getString(Config.ALLOWED_IPS).split(COMMA));

        if (isOnNetwork(new Ip4(originalClientIP), allowedIPs)) {
            final String receivedQuery = path.concat(
                    aContext.request().query() != null ? QUESTION_MARK.concat(aContext.request().query()) : EMPTY);
            myTokenProxy.getBearerToken().compose(token -> {
                return myApiProxy.getLibCalOutput(token, SLASH.concat(receivedQuery)).onSuccess(apiOutput -> {
                    response.setStatusCode(apiOutput.getInteger(JsonKeys.STATUS_CODE));
                    response.setStatusMessage(apiOutput.getString(JsonKeys.STATUS_MESSAGE));
                    response.putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
                    response.end(apiOutput.getString(JsonKeys.BODY));
                });
            }).onFailure(failure -> {
                returnError(response, HTTP.INTERNAL_SERVER_ERROR, failure.getMessage());
            });
        } else {
            returnError(response, HTTP.FORBIDDEN, LOGGER.getMessage(MessageCodes.LCP_007, originalClientIP));
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
                LOGGER.getMessage(MessageCodes.LCP_006, aError.replaceAll(Constants.EOL_REGEX, Constants.BR_TAG)));

        aResponse.setStatusCode(aStatusCode);
        aResponse.setStatusMessage(aError.replaceAll(Constants.EOL_REGEX, EMPTY));
        aResponse.putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
        aResponse.end(errorBody.encodePrettily());
    }

    /**
     * Builds a collection of authorized subnets in a network.
     *
     * @param anIpArray An array of IP addresses/ranges
     * @return The converted collection of allowed subnets in network
     */
    private Cidr4Trie<String> buildAllowedNetwork(final String... anIpArray) {
        final Cidr4Trie<String> allowedNetwork = new Cidr4Trie<>();
        for (final String address : anIpArray) {
            allowedNetwork.put(new Cidr4(address), address);
        }
        return allowedNetwork;
    }

    /**
     * Checks if an IP address belongs to a network.
     *
     * @param aIpAddress The IP address
     * @param aNetworkSubnets The collection of subnets that defines a network
     * @return Whether the IP address belongs to any subnet in the collection
     */
    private boolean isOnNetwork(final Ip4 aIpAddress, final Cidr4Trie<String> aNetworkSubnets) {
        return aNetworkSubnets.shortestPrefixOfValue(new Cidr4(aIpAddress), true) != null;
    }
}
