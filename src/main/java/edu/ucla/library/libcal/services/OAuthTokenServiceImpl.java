
package edu.ucla.library.libcal.services;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.JsonKeys;
import edu.ucla.library.libcal.MessageCodes;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;

/**
 * Implementation of {@link OAuthTokenService}.
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
     * The OAuth authentication providers.
     */
    private final Queue<OAuth2Auth> myAuthProviders = new LinkedList<>();

    /**
     * The ID of the periodic timer for checking if the OAuth token needs refreshing.
     */
    private long myTimerId;

    /**
     * Creates an instance of the service. Use {@link OAuthTokenService#create(Vertx, JsonObject)} to invoke.
     * <p>
     * Note that the return value should not be used directly. Instead use the result of {@code aPromise}.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A configuration
     * @param aPromise A Promise that completes with the service instance if initialization was successful, or fails
     *        otherwise
     */
    public OAuthTokenServiceImpl(final Vertx aVertx, final JsonObject aConfig,
            final Promise<OAuthTokenService> aPromise) {
        final JsonObject baseOptions = new OAuth2Options().setFlow(OAuth2FlowType.CLIENT)
                .setSite(aConfig.getString(Config.OAUTH_TOKEN_URL)).toJson();
        final OAuth2Options options1 = new OAuth2Options(baseOptions)
                .setClientId(aConfig.getString(Config.OAUTH_CLIENT1_ID))
                .setClientSecret(aConfig.getString(Config.OAUTH_CLIENT1_SECRET));
        final OAuth2Options options2 = new OAuth2Options(baseOptions)
                .setClientId(aConfig.getString(Config.OAUTH_CLIENT2_ID))
                .setClientSecret(aConfig.getString(Config.OAUTH_CLIENT2_SECRET));

        myVertx = aVertx;
        myAuthProviders.add(OAuth2Auth.create(aVertx, options1));
        myAuthProviders.add(OAuth2Auth.create(aVertx, options2));

        authenticateWithRetry(Optional.of(1), 5).compose(token -> {
            myTimerId = keepTokenFresh(token, 300);

            LOGGER.debug(MessageCodes.LCP_002,
                    ((OAuth2AuthProviderImpl) myAuthProviders.peek()).getConfig().getClientId(),
                    token.principal().encodePrettily());

            myAuthProviders.add(myAuthProviders.remove());

            return shareAccessToken(token);
        }).onSuccess(unused -> aPromise.complete(this)).onFailure(aPromise::fail);
    }

    @Override
    public Future<String> getBearerToken() {
        return myVertx.sharedData().getLocalAsyncMap(Constants.ACCESS_TOKEN_MAP).compose(asyncMap -> {
            return asyncMap.get(Constants.ACCESS_TOKEN).map(token -> (String) token);
        });
    }

    @Override
    public Future<Void> close() {
        myVertx.cancelTimer(myTimerId);

        return Future.succeededFuture();
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

    /**
     * Refresh the OAuth token before it has a chance to expire. This method is recursive.
     *
     * @param aToken The OAuth token
     * @param aSecondsBeforeExpiration The number of seconds before expiration to attempt a refresh
     * @return The ID of a Vert.x timer
     */
    private long keepTokenFresh(final User aToken, final int aSecondsBeforeExpiration) {
        final int delay = aToken.principal().getInteger(JsonKeys.EXPIRES_IN) - aSecondsBeforeExpiration;

        return myVertx.setTimer(delay * 1000, timerID -> {
            authenticateWithRetry(Optional.of(3), 5).compose(newToken -> {
                myTimerId = keepTokenFresh(newToken, aSecondsBeforeExpiration);

                LOGGER.debug(MessageCodes.LCP_002,
                        ((OAuth2AuthProviderImpl) myAuthProviders.peek()).getConfig().getClientId(),
                        newToken.principal().encodePrettily());

                myAuthProviders.add(myAuthProviders.remove());

                return shareAccessToken(newToken);
            }).onFailure(details -> LOGGER.error(MessageCodes.LCP_005, details.getMessage()));
        });
    }

    /**
     * Attempts authentication with a set number of retries.
     *
     * @param aRetryCount The optional number of times to retry (retries forever if empty)
     * @param aRetryDelay The number of seconds to wait between retry attempts
     * @return A Future that succeeds with the new OAuth token if authentication is successful, or fails otherwise
     */
    private Future<User> authenticateWithRetry(final Optional<Integer> aRetryCount, final int aRetryDelay) {
        final Promise<User> authentication = Promise.promise();

        authenticateWithRetryHelper(aRetryCount, aRetryDelay, authentication);

        return authentication.future();
    }

    /**
     * Helper function for hiding the recursion.
     *
     * @param aRetryCount The optional number of times to retry (retries forever if empty)
     * @param aRetryDelay The number of seconds to wait between retry attempts
     * @param aPromise A Promise that completes with the new OAuth token if authentication is successful, or fails
     *        otherwise
     */
    private void authenticateWithRetryHelper(final Optional<Integer> aRetryCount, final int aRetryDelay,
            final Promise<User> aPromise) {
        myAuthProviders.peek().authenticate(new JsonObject()).onSuccess(aPromise::complete).onFailure(failure -> {
            if (aRetryCount.isEmpty() || aRetryCount.get() > 0) {
                // Wait a bit before retrying again
                myVertx.setTimer(aRetryDelay * 1000, timerID -> {
                    authenticateWithRetryHelper(aRetryCount.map(count -> count - 1), aRetryDelay, aPromise);
                });

                LOGGER.warn(MessageCodes.LCP_004, failure.getMessage(), aRetryDelay);
            } else {
                aPromise.fail(failure);
            }
        });
    }
}
