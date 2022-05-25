
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
    public Future<String> getLibCalOutput(final String anOAuthToken, final String aQuery) {
        /*
         * for some reason, JsonObject has trouble parsing the output from api/1.1/hours/[id]
         * haven't tried with other API calls, buut seems safer to handle API output as string
         */
        final Promise<String> promise = Promise.promise();
        final HttpRequest<String> request;
        final StringBuilder responseBody = new StringBuilder();
        final String baseURL = myConfig.getString(Config.LIBCAL_BASE_URL);

        request = WebClient.create(myVertx).getAbs(baseURL.concat(aQuery)).bearerTokenAuthentication(anOAuthToken)
                .as(BodyCodec.string()).expect(ResponsePredicate.SC_OK).ssl(true);
        request.send(asyncResult -> {
            if (asyncResult.succeeded()) {
                responseBody.append(asyncResult.result().body());
                promise.complete(responseBody.toString());
            } else {
                responseBody.append("cause: ".concat(asyncResult.cause().getMessage()));
                if (asyncResult.result() != null) {
                    responseBody.append("status: ".concat(asyncResult.result().statusMessage()));
                }
                promise.fail(responseBody.toString());
            }
        });

        return promise.future();
    }

    @Override
    public Future<Void> close() {
        return Future.succeededFuture();
    }
}
