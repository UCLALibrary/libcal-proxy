
package edu.ucla.library.libcal.services;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.HttpResponseMapper;

import io.vertx.core.Future;
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
     * The LibCal base URL.
     */
    private final String myLibCalBaseURL;

    /**
     * The HTTP response serializer.
     */
    private final HttpResponseMapper myMapper = new HttpResponseMapper();

    LibCalProxyServiceImpl(final Vertx aVertx, final JsonObject aConfig) {
        myLibCalBaseURL = aConfig.getString(Config.LIBCAL_BASE_URL);
        myWebClient = WebClient.create(aVertx);
    }

    @Override
    public Future<JsonObject> getLibCalOutput(final String anOAuthToken, final String aQuery) {
        /*
         * LibCal API returns JSON in variable formats (sometimes objects, sometimes arrays), so safer to handle API
         * output as string to avoid parsing errors
         */
        final HttpRequest<String> request = myWebClient.getAbs(myLibCalBaseURL.concat(aQuery))
                .bearerTokenAuthentication(anOAuthToken).as(BodyCodec.string()).ssl(true);

        return request.send().map(myMapper::encode);
    }

    @Override
    public Future<JsonObject> postLibCalOutput(String anOAuthToken, String aQuery, JsonObject aBody) {
        final HttpRequest<String> request = myWebClient.postAbs(myLibCalBaseURL.concat(aQuery))
                .bearerTokenAuthentication(anOAuthToken).as(BodyCodec.string()).ssl(true);

        return request.sendJsonObject(aBody).map(myMapper::encode);
    }
}
