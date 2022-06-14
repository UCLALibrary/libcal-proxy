
package edu.ucla.library.libcal.services;

import static info.freelibrary.util.HTTP.BAD_REQUEST;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.JsonKeys;

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
    public Future<JsonObject> getLibCalOutput(final String anOAuthToken, final String aQuery) {
        /*
         * LibCal API returns JSON in variable formats (sometimes objects, sometimes arrays), so safer to handle API
         * output as string to avoid parsing errors
         */
        final Promise<JsonObject> promise = Promise.promise();
        final HttpRequest<String> request;
        final JsonObject response = new JsonObject();
        final String baseURL = myConfig.getString(Config.LIBCAL_BASE_URL);

        request = myWebClient.getAbs(baseURL.concat(aQuery)).bearerTokenAuthentication(anOAuthToken)
                .as(BodyCodec.string()).expect(ResponsePredicate.SC_OK).ssl(true);
        request.send(asyncResult -> {
            if (asyncResult.succeeded()) {
                response.put(JsonKeys.STATUS_CODE, asyncResult.result().statusCode());
                response.put(JsonKeys.STATUS_MESSAGE, asyncResult.result().statusMessage());
                if (asyncResult.result().statusCode() < BAD_REQUEST) {
                    response.put(JsonKeys.BODY, asyncResult.result().body());
                }
                promise.complete(response);
            } else {
                promise.fail(asyncResult.cause());
            }
        });

        return promise.future();
    }

}
