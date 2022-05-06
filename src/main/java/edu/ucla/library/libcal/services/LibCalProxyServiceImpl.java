package edu.ucla.library.libcal.services;

import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.JsonKeys;
import edu.ucla.library.libcal.utils.TokenUtils;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The implementation of LibCalProxyService.
 */
public class LibCalProxyServiceImpl implements LibCalProxyService {

    /**
     * The name of the access token stored in shared data.
     */
    private static final String CURRENT_TOKEN = "currentToken";

    /**
     * A Vert.x vertx instance used by various methods
     */
    private final Vertx myVertx;

    /**
     * Creates an instance of the service.
     *
     * @param aVertx A Vert.x vertx instance
     */
    LibCalProxyServiceImpl(final Vertx aVertx) {
        myVertx = aVertx;
    }

    @Override
    public Future<JsonObject> getConfig() {
        final Promise<JsonObject> promise = Promise.promise();
        final ConfigStoreOptions envPropsStore = new ConfigStoreOptions().setType("env");
        final ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys");
        final ConfigRetrieverOptions options = new ConfigRetrieverOptions()
            .addStore(envPropsStore).addStore(sysPropsStore);
        final ConfigRetriever retriever = ConfigRetriever.create(myVertx, options);

        retriever.getConfig(configResult -> {
            if (configResult.succeeded()) {
                promise.complete(configResult.result());
            } else {
                promise.fail(configResult.cause().getMessage());
            }
        });

        return promise.future();
    }

    @Override
    public Future<JsonObject> getAccessToken(final String aClientID, final String aClientSecret,
                              final String aTokenURL) {
        final LocalMap<String, JsonObject> tokenMap = myVertx.sharedData().getLocalMap(Constants.SHARED_TOKEN);
        if (tokenMap.containsValue(CURRENT_TOKEN)) {
            final JsonObject theToken = tokenMap.get(CURRENT_TOKEN);
            if (!hasExpired(theToken.getString(JsonKeys.EXPIRES_AT))) {
                return Future.succeededFuture(theToken);
            } else {
                return getNewToken(aClientID, aClientSecret, aTokenURL).compose(newToken -> {
                    tokenMap.put(CURRENT_TOKEN, newToken);
                    return Future.succeededFuture(newToken);
                });
            }
        } else {
            return getNewToken(aClientID, aClientSecret, aTokenURL).compose(newToken -> {
                tokenMap.put(CURRENT_TOKEN, newToken);
                return Future.succeededFuture(newToken);
            });
        }
    }

    private Future<JsonObject> getNewToken(final String aClientID, final String aClientSecret, final String aTokenURL) {
        final JsonObject clientInfo = new JsonObject()
               .put(JsonKeys.CLIENT_ID, aClientID)
               .put(JsonKeys.CLIENT_SECRET, aClientSecret)
               .put(JsonKeys.TOKEN_ENDPOINT, aTokenURL);
        return TokenUtils.getAccessToken(clientInfo, myVertx);
    }

    private boolean hasExpired(final String aExpireTime) {
        return LocalDateTime.now().isAfter(LocalDateTime.from(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(aExpireTime)));
    }

    @Override
    public Future<JsonObject> getLibCalOutput(final String aOAuthToken, final String aBaseURL, final String aQuery) {
        final Promise<JsonObject> promise = Promise.promise();
        final HttpRequest<JsonObject> request;
        final JsonObject responseBody = new JsonObject();

        request = WebClient.create(myVertx).getAbs(aBaseURL.concat(aQuery))
                .bearerTokenAuthentication(aOAuthToken)
                .as(BodyCodec.jsonObject()).expect(ResponsePredicate.SC_OK).ssl(true);
        request.send(asyncResult -> {
            if (asyncResult.succeeded()) {
                responseBody.mergeIn(asyncResult.result().body());
                promise.complete(responseBody);

            } else {
                responseBody.put("cause", asyncResult.cause().getMessage());
                responseBody.put("status", asyncResult.result().statusMessage());
                promise.fail(responseBody.encodePrettily());
            }
        });

        return promise.future();
    }
}
