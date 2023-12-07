
package edu.ucla.library.libcal.services;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.JsonKeys;
import edu.ucla.library.libcal.MessageCodes;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.client.WebClient;
import io.vertx.uritemplate.UriTemplate;

/**
 * Implementation of {@link OAuthTokenService}.
 */
public class OAuthTokenServiceImpl implements OAuthTokenService {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthTokenServiceImpl.class, MessageCodes.BUNDLE);

    /**
     * The JSON key of the client ID.
     */
    private static final String CLIENT_ID = "client_id";

    /**
     * The Vert.x instance.
     */
    private final Vertx myVertx;

    /**
     * The client credentials.
     */
    private final Queue<JsonObject> myClientCredentials = new LinkedList<>();

    /**
     * An HTTP client for calling the access token service.
     */
    private final WebClient myWebClient;

    /**
     * The location of the access token service.
     */
    private final UriTemplate myAccessTokenService;

    /**
     * See {@link Config#LIBCAL_AUTH_RETRY_COUNT}.
     */
    private final Optional<Integer> myAuthRetryCount;

    /**
     * See {@link Config#LIBCAL_AUTH_RETRY_DELAY}.
     */
    private final int myAuthRetryDelay;

    /**
     * See {@link Config#LIBCAL_AUTH_EXPIRES_IN_PADDING}.
     */
    private final int myAuthExpiresInPadding;

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
        final JsonObject clientCredentials1 =
                createCredentials(Integer.parseInt(aConfig.getString(Config.OAUTH_CLIENT1_ID)),
                        aConfig.getString(Config.OAUTH_CLIENT1_SECRET));
        final JsonObject clientCredentials2 =
                createCredentials(Integer.parseInt(aConfig.getString(Config.OAUTH_CLIENT2_ID)),
                        aConfig.getString(Config.OAUTH_CLIENT2_SECRET));

        myAccessTokenService = UriTemplate.of(aConfig.getString(Config.OAUTH_TOKEN_URL));
        myVertx = aVertx;
        myWebClient = WebClient.create(aVertx);
        myClientCredentials.add(clientCredentials1);
        myClientCredentials.add(clientCredentials2);
        myAuthRetryCount = Optional.ofNullable(aConfig.getInteger(Config.LIBCAL_AUTH_RETRY_COUNT, null));
        myAuthRetryDelay = aConfig.getInteger(Config.LIBCAL_AUTH_RETRY_DELAY, 10);
        myAuthExpiresInPadding = aConfig.getInteger(Config.LIBCAL_AUTH_EXPIRES_IN_PADDING, 300);

        authenticateWithRetry().compose(this::postAuthenticate).onSuccess(unused -> aPromise.complete(this))
                .onFailure(aPromise::fail);
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
    private Future<Void> shareAccessToken(final User aToken) {
        return myVertx.sharedData().getLocalAsyncMap(Constants.ACCESS_TOKEN_MAP).compose(asyncMap -> {
            return asyncMap.put(Constants.ACCESS_TOKEN, aToken.principal().getString(JsonKeys.ACCESS_TOKEN));
        });
    }

    /**
     * Refresh the OAuth token before it has a chance to expire.
     *
     * @param aToken The OAuth token
     * @return The ID of a Vert.x timer
     */
    private long keepTokenFresh(final User aToken) {
        final int delay = aToken.principal().getInteger(JsonKeys.EXPIRES_IN) - myAuthExpiresInPadding;

        return myVertx.setTimer(delay * 1000, timerID -> {
            authenticateWithRetry().compose(this::postAuthenticate)
                    .onFailure(details -> LOGGER.error(MessageCodes.LCP_005, details.getMessage()));
        });
    }

    /**
     * Attempts authentication with a set number of retries.
     *
     * @return A Future that succeeds with the new OAuth token if authentication is successful, or fails otherwise
     */
    private Future<User> authenticateWithRetry() {
        final Promise<User> authentication = Promise.promise();

        authenticateWithRetryHelper(myAuthRetryCount, authentication);

        return authentication.future();
    }

    /**
     * Performs the recursion for {@link #authenticateWithRetry()}.
     *
     * @param aRetryCount The optional number of times to retry (retries forever if empty)
     * @param aPromise A Promise that completes with the new OAuth token if authentication is successful, or fails
     *        otherwise
     */
    private void authenticateWithRetryHelper(final Optional<Integer> aRetryCount, final Promise<User> aPromise) {
        myWebClient.postAbs(myAccessTokenService).sendJsonObject(myClientCredentials.peek()).onSuccess(response -> {
            if (response.statusCode() == HTTP.OK) {
                aPromise.complete(User.create(response.bodyAsJsonObject()));
            } else {
                if (aRetryCount.isEmpty() || aRetryCount.get() > 0) {
                    // Wait a bit before retrying again
                    myVertx.setTimer(myAuthRetryDelay * 1000, timerID -> {
                        authenticateWithRetryHelper(aRetryCount.map(count -> count - 1), aPromise);
                    });

                    LOGGER.warn(MessageCodes.LCP_004, response.bodyAsString(), myAuthRetryDelay);
                } else {
                    aPromise.fail(response.bodyAsString());
                }
            }
        }).onFailure(aPromise::fail);
    }

    /**
     * Updates application state to reflect the newly-acquired OAuth token.
     *
     * @param aToken The OAuth token
     * @return A Future that succeeds once the application state has been updated, or fails if there was a problem
     */
    private Future<Void> postAuthenticate(final User aToken) {
        myTimerId = keepTokenFresh(aToken);

        LOGGER.debug(MessageCodes.LCP_002, myClientCredentials.peek().getString(CLIENT_ID),
                aToken.principal().encodePrettily());

        // Send the just-used client credentials to the back of the queue
        myClientCredentials.add(myClientCredentials.remove());

        return shareAccessToken(aToken);
    }

    /**
     * Creates a JsonObject containing LibCal API credentials based on the provided client ID and client secret.
     *
     * @param aClientID A client ID
     * @param aClientSecret A client secret
     * @return A JSON payload to send to the access token service
     */
    private static JsonObject createCredentials(final int aClientID, final String aClientSecret) {
        return new JsonObject().put(CLIENT_ID, aClientID).put("client_secret", aClientSecret).put("grant_type",
                "client_credentials");
    }
}
