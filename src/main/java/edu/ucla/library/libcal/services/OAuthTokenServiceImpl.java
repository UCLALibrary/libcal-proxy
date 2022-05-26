
package edu.ucla.library.libcal.services;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.JsonKeys;
import edu.ucla.library.libcal.MessageCodes;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;

/**
 * Implementation of {@link OAuthTokenService}.
 * <p>
 * FIXME: the likelihood of {@link OAuthTokenService#getBearerToken(Vertx)} returning an expired token is probably too
 * high for a production service.
 */
public class OAuthTokenServiceImpl implements OAuthTokenService {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthTokenServiceImpl.class, MessageCodes.BUNDLE);

    /**
     * The Vert.x instance.
     */
    private final Vertx myVertx;

    /**
     * The ID of the periodic timer for checking if the OAuth token needs refreshing.
     */
    private final long myTimerId;

    /**
     * The OAuth authentication provider.
     */
    private final OAuth2Auth myAuthProvider;

    /**
     * LibCal credentials represented as an OAuth token.
     */
    private User myToken;

    /**
     * Creates an instance of the service. Use {@link OAuthTokenService#create(Vertx, JsonObject)} to invoke.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A configuration
     * @param anAuthProvider An OAuth authentication provider
     * @param aToken A token that was returned by authenticating against {@code anAuthProvider}
     */
    public OAuthTokenServiceImpl(final Vertx aVertx, final JsonObject aConfig, final OAuth2Auth anAuthProvider,
            final User aToken) {
        myVertx = aVertx;
        myAuthProvider = anAuthProvider;
        myToken = aToken;
        myTimerId = aVertx.setPeriodic(1000, timerId -> {
            if (myToken.expired()) {
                refreshTokenIfExpired().onFailure(details -> LOGGER.error(details.getMessage()));
            }
        });

        LOGGER.debug(MessageCodes.LCP_002, aConfig.getString(Config.OAUTH_CLIENT_ID),
                aToken.principal().encodePrettily());
    }

    @Override
    public Future<String> getBearerToken() {
        return myVertx.sharedData().getLocalAsyncMap(Constants.ACCESS_TOKEN_MAP).compose(asyncMap -> {
            return asyncMap.get(Constants.ACCESS_TOKEN).map(token -> (String) token);
        });
    }

    /**
     * Requests that the OAuth token managed by this service is refreshed.
     *
     * @return A Future that succeeds when a valid token is available via {@link #getBearerToken(Vertx)}, or fails if a
     *         new token could not be obtained
     */
    protected Future<User> refreshTokenIfExpired() {
        if (myToken.expired()) {
            // FIXME: retry on failure; see https://vertx.io/docs/vertx-auth-oauth2/java/#_refresh_token
            return myAuthProvider.authenticate(new JsonObject()).compose(newUser -> {
                myToken = newUser;

                return shareAccessToken(newUser).map(newUser);
            });
        } else {
            return Future.succeededFuture();
        }
    }

    /**
     * Make the access token accessible to the rest of the application via {@link OAuthTokenService#getAccessToken}.
     *
     * @param aToken An OAuth token
     * @return A Future that resolves once the new value has been put into the map
     */
    protected Future<Void> shareAccessToken(final User aToken) {
        return myVertx.sharedData().getLocalAsyncMap(Constants.ACCESS_TOKEN_MAP).compose(asyncMap -> {
            return asyncMap.put(Constants.ACCESS_TOKEN, aToken.principal().getString(JsonKeys.ACCESS_TOKEN));
        });
    }

    @Override
    public Future<Void> close() {
        myVertx.cancelTimer(myTimerId);

        return Future.succeededFuture();
    }
}
