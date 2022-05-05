
package edu.ucla.library.libcal.utils;

import edu.ucla.library.libcal.JsonKeys;
import edu.ucla.library.libcal.MessageCodes;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;

/**
 * Utility method for retrieving OAuth access tokens.
 */
public final class TokenUtils {

    /** The logger used by the token utilities. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenUtils.class, MessageCodes.BUNDLE);

    /**
     * Private constructor for TokenUtils class.
     */
    private TokenUtils() {
    }

    /**
     * Retrieves the bearer token from LibCal OAuth provider response.
     *
     * @param aClientInfo JSON object holding client ID and secret
     * @param aVertx A Vertx object used to build token request
     * @return JSON object wrapping the access token
     */
    public static Future<JsonObject> getAccessToken(final JsonObject aClientInfo, final Vertx aVertx) {
        return handleRawToken(aClientInfo, aVertx).compose(rawToken -> {
            final JsonObject accessToken = new JsonObject();
            LOGGER.info(MessageCodes.LCP_002, accessToken.encodePrettily());
	    if (rawToken.containsKey(JsonKeys.ACCESS_TOKEN)) {
                accessToken.put(JsonKeys.ACCESS_TOKEN, rawToken.getString(JsonKeys.ACCESS_TOKEN));
	    } else {
                accessToken.put(JsonKeys.TOKEN_ERROR, rawToken.toString());
	    }
            return Future.succeededFuture(accessToken);
        });
    }

    /**
     * Retrieves an access token from LibCal OAuth provider.
     *
     * @param aClientInfo JSON object holding client ID and secret
     * @param aVertx A Vertx object used to build token request
     * @return JSON object wrapping the access token
     */
    private static Future<JsonObject> handleRawToken(final JsonObject aClientInfo, final Vertx aVertx) {
        final Promise<JsonObject> promise = Promise.promise();

        final OAuth2Options credentials = new OAuth2Options().setFlow(OAuth2FlowType.CLIENT)
                .setClientId(aClientInfo.getString(JsonKeys.CLIENT_ID))
                .setClientSecret(aClientInfo.getString(JsonKeys.CLIENT_SECRET))
                .setSite(aClientInfo.getString(JsonKeys.TOKEN_ENDPOINT));

        final OAuth2Auth oauth2 = OAuth2Auth.create(aVertx, credentials);

        final JsonObject tokenConfig = new JsonObject();
        oauth2.authenticate(tokenConfig).onSuccess(user -> {
            promise.complete(user.principal());
        }).onFailure(err -> {
            LOGGER.error(MessageCodes.LCP_003, err.getMessage());
            promise.fail(err.getMessage());
        });
        return promise.future();
    }
}
