
package edu.ucla.library.libcal.services;

import java.util.Optional;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.JsonKeys;
import edu.ucla.library.libcal.MessageCodes;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
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
     * The OAuth authentication provider.
     */
    private final OAuth2Auth myAuthProvider;

    /**
     * The ID of the periodic timer for checking if the OAuth token needs refreshing.
     */
    private long myTimerId;

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
        myTimerId = keepTokenFresh(aToken, 300);

        LOGGER.debug(MessageCodes.LCP_002, aConfig.getString(Config.OAUTH_CLIENT_ID),
                aToken.principal().encodePrettily());
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

        return myVertx.setTimer(delay, timerID -> {
            authenticateWithRetry(Optional.of(3), 5000, Promise.promise(), authentication -> {
                if (authentication.succeeded()) {
                    final User newToken = authentication.result();

                    myTimerId = keepTokenFresh(newToken, aSecondsBeforeExpiration);

                    shareAccessToken(newToken);
                } else {
                    LOGGER.error(authentication.cause().getMessage());
                }
            });
        });
    }

    /**
     * Attempts authentication with a set number of retries. This method is recursive.
     *
     * @param aRetryCount The optional number of times to retry (retries forever if empty)
     * @param aRetryDelay The time to wait between retry attempts, in milliseconds
     * @param aPromise The promise that will be completed if authentication succeeds, or failed otherwise
     * @param aResultHandler The handler that will be called with the result of {@code aPromise}
     */
    private void authenticateWithRetry(final Optional<Integer> aRetryCount, final long aRetryDelay,
            final Promise<User> aPromise, final Handler<AsyncResult<User>> aResultHandler) {
        myAuthProvider.authenticate(new JsonObject()).onFailure(failure -> {
            if (aRetryCount.isEmpty() || aRetryCount.get() > 0) {
                // Wait a bit before retrying again
                myVertx.executeBlocking(promise -> {
                    try {
                        Thread.sleep(aRetryDelay);
                        promise.complete();
                    } catch (final InterruptedException details) {
                        promise.fail(details);
                    }
                }, result -> {
                    authenticateWithRetry(aRetryCount.map(count -> count - 1), aRetryDelay, aPromise, aResultHandler);
                });
            } else {
                aPromise.fail(failure);
            }
        });
    }
}
