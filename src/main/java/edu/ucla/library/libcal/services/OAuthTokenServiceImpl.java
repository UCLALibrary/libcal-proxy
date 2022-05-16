
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

        shareAccessToken(aToken);

        LOGGER.debug(MessageCodes.LCP_002, aConfig.getString(Config.OAUTH_CLIENT_ID),
                aToken.principal().encodePrettily());
    }

    @Override
    public Future<Void> refreshTokenIfExpired() {
        if (myToken.expired()) {
            // FIXME: retry on failure; see https://vertx.io/docs/vertx-auth-oauth2/java/#_refresh_token
            return myAuthProvider.authenticate(new JsonObject()).map(newUser -> {
                myToken = newUser;

                shareAccessToken(newUser);

                return null;
            });
        } else {
            return Future.succeededFuture();
        }
    }

    /**
     * Make the access token accessible to the rest of the application via {@link OAuthTokenService#getAccessToken}.
     *
     * @param aToken An OAuth token
     */
    private void shareAccessToken(final User aToken) {
        myVertx.sharedData().getLocalMap(Constants.ACCESS_TOKEN_MAP).put(Constants.ACCESS_TOKEN,
                aToken.principal().getString(JsonKeys.ACCESS_TOKEN));
    }

    @Override
    public Future<Void> close() {
        myVertx.cancelTimer(myTimerId);

        return Future.succeededFuture();
    }
}
