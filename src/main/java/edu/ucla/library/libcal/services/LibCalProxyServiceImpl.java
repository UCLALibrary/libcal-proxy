
package edu.ucla.library.libcal.services;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.HttpResponseMapper;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

/**
 * The implementation of LibCalProxyService.
 */
@SuppressWarnings("PMD.UseObjectForClearerAPI")
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

    /**
     * Creates a new instance of the LibCalProxyServiceImpl.
     *
     * @param aVertx   A Vert.x instance
     * @param aConfig  A configuration
     * @throws NullPointerException if either aVertx or aConfig is null.
     */
    LibCalProxyServiceImpl(final Vertx aVertx, final JsonObject aConfig) {
        myLibCalBaseURL = aConfig.getString(Config.LIBCAL_BASE_URL);
        myWebClient = WebClient.create(aVertx);
    }

    @Override
    public Future<JsonObject> getLibCalOutput(final String anOAuthToken, final String aQuery, final String aMethod,
            final String aBody) {
        /*
         * LibCal API returns JSON in variable formats (sometimes objects, sometimes arrays), so safer to handle API
         * output as string to avoid parsing errors
         */
        final HttpRequest<String> request =
                myWebClient.requestAbs(HttpMethod.valueOf(aMethod), myLibCalBaseURL.concat(aQuery))
                        .bearerTokenAuthentication(anOAuthToken).as(BodyCodec.string()).ssl(true);

        return aBody != null ? request.sendBuffer(Buffer.buffer(aBody)).map(myMapper::encode)
                : request.send().map(myMapper::encode);
    }

}
