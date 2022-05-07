
package edu.ucla.library.libcal.handlers;

import static edu.ucla.library.libcal.MediaType.APPLICATION_JSON;
  
import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.JsonKeys;
import edu.ucla.library.libcal.MessageCodes;
import edu.ucla.library.libcal.services.LibCalProxyService;

import io.vertx.core.Future;
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

    /*
     * The logger used by the token utilities.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHandler.class, MessageCodes.BUNDLE);

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
        final String receivedApp = aContext.pathParam(Constants.APP_PARAM);
        final String receivedQuery = aContext.pathParam(Constants.QUERY_PARAM);

        if (receivedApp == null || receivedApp.equals(Constants.EMPTY)) {
            LOGGER.error(MessageCodes.LCP_004);
            response.setStatusCode(HTTP.BAD_REQUEST).end(LOGGER.getMessage(MessageCodes.LCP_004));
            return;
        } else if (receivedQuery == null || receivedQuery.equals(Constants.EMPTY)) {
            LOGGER.error(MessageCodes.LCP_005);
            response.setStatusCode(HTTP.BAD_REQUEST).end(LOGGER.getMessage(MessageCodes.LCP_005));
            return;
        } else {
            /*
	     * I've probably made a hash of the following... though it is syntactiacally correct
	     * If the intent is not clear: 
	     *   retrieve config from the LibCalProxyService
	     *   retrieve client credentials and assorted URLs from config
	     *   retrieve a OAuth bearer token from the LibCalProxyService
	     *   use the token to call the client-requested LibCal API and feed results back to client
	     */
            myProxyService.getConfig().compose(config-> {
		//assumption is ID/secret for particular profile is stored in env as e.g., client_secret_dsc_libcal_readonly
                final String clientID = config.getString(JsonKeys.CLIENT_ID.concat(Constants.UNDERSCORE).concat(receivedApp));
                final String clientSecret = config.getString(JsonKeys.CLIENT_SECRET.concat(Constants.UNDERSCORE).concat(receivedApp));
                final String oauthEndpoint = config.getString(Config.OAUTH_TOKEN_URL);
		final String baseURL = config.getString(Config.LIBCAL_BASE_URL);
                myProxyService.getAccessToken(clientID, clientSecret, oauthEndpoint).compose(token -> {
                    myProxyService.getLibCalOutput(token.getString(JsonKeys.ACCESS_TOKEN), baseURL, receivedQuery).compose(apiOutput -> {
                        response.setStatusCode(HTTP.OK).putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
			response.end(apiOutput.encodePrettily());
                        return Future.future(null);
		    });
                    return Future.future(null);
		});
		return Future.future(null);
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
