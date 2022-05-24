
package edu.ucla.library.libcal.services;

import edu.ucla.library.libcal.Config;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;

/**
 * The implementation of LibCalProxyService.
 */
public class LibCalProxyServiceImpl implements LibCalProxyService {

    /**
     * A Vert.x instance.
     */
    private final Vertx myVertx;

    /**
     * App config in Json format.
     */
    private final JsonObject myConfig;

    LibCalProxyServiceImpl(final Vertx aVertx, final JsonObject aConfig) {
        myVertx = aVertx;
        myConfig = aConfig;
    }

    @Override
    public Future<JsonObject> getLibCalOutput(final String anOAuthToken, final String aQuery) {
        final Promise<JsonObject> promise = Promise.promise();
        final HttpRequest<JsonObject> request;
        final JsonObject responseBody = new JsonObject();
        final String baseURL = myConfig.getString(Config.LIBCAL_BASE_URL);

        request = WebClient.create(myVertx).getAbs(baseURL.concat(aQuery)).bearerTokenAuthentication(anOAuthToken)
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

    @Override
    public Future<Void> close() {
        return Future.succeededFuture();
    }
}
