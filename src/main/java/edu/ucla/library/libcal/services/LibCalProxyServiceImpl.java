
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
     * HTTP client for retrieving LibCal output.
     */
    private final WebClient myWebClient;

    /**
     * App config in Json format.
     */
    private final JsonObject myConfig;

    LibCalProxyServiceImpl(final Vertx aVertx, final JsonObject aConfig) {
        myConfig = aConfig;
        myWebClient = WebClient.create(aVertx);
    }

    @Override
    public Future<String> getLibCalOutput(final String anOAuthToken, final String aQuery) {
        /*
         * LibCal API returns JSON in variable formats (sometimes objects, sometimes arrays),
         * so safer to handle API output as string to avoid parsing errors
         */
        final Promise<String> promise = Promise.promise();
        final HttpRequest<String> request;
        final StringBuilder responseBody = new StringBuilder();
        final String baseURL = myConfig.getString(Config.LIBCAL_BASE_URL);

        request = myWebClient.getAbs(baseURL.concat(aQuery)).bearerTokenAuthentication(anOAuthToken)
                .as(BodyCodec.string()).expect(ResponsePredicate.SC_OK).ssl(true);
        request.send(asyncResult -> {
            if (asyncResult.succeeded()) {
                responseBody.append(asyncResult.result().body());
                promise.complete(responseBody.toString());
            } else {
                promise.fail(asyncResult.cause());
            }
        });

        return promise.future();
    }

    @Override
    public Future<Void> close() {
        return Future.succeededFuture();
    }
}
