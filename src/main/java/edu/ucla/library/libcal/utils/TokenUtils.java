
package edu.ucla.library.libcal.utils;

import edu.ucla.library.libcal.Constants;

import io.vertx.core.Vertx;

/**
 * Utility method for retrieving OAuth access tokens.
 */
public final class TokenUtils {

    /**
     * Private constructor for TokenUtils class.
     */
    private TokenUtils() {
    }

    /**
     * Retrieves the access token to use for authorizing HTTP requests to the LibCal API.
     *
     * @param aVertx A Vert.x instance
     * @return The access token
     */
    public static String getAccessToken(final Vertx aVertx) {
        return (String) aVertx.sharedData().getLocalMap(Constants.ACCESS_TOKEN_MAP).get(Constants.ACCESS_TOKEN);
    }
}
