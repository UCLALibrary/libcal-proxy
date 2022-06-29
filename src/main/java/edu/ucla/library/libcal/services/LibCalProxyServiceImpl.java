
package edu.ucla.library.libcal.services;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.HttpResponseMapper;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
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

    /**
     * The HTTP response serializer.
     */
    private final HttpResponseMapper myMapper = new HttpResponseMapper();

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
        final String baseURL = myConfig.getString(Config.LIBCAL_BASE_URL);
        final HttpRequest<String> request = myWebClient.getAbs(baseURL.concat(aQuery))
                .bearerTokenAuthentication(anOAuthToken).as(BodyCodec.string()).ssl(true);

        request.send(asyncResult -> {
            if (asyncResult.succeeded()) {
                promise.complete(myMapper.encode(asyncResult.result()));
            } else {
                promise.fail(asyncResult.cause());
            }
        });

        return promise.future();
    }

}
