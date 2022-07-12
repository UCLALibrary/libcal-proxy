
package edu.ucla.library.libcal;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.HttpResponse;

import java.util.List;

/**
 * A class that implements the {@link HttpResponse} interface.
 */
public class HttpResponseImpl implements HttpResponse<String> {

    /**
     * The class's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpResponseImpl.class, MessageCodes.BUNDLE);

    /**
     * The HTTP protocol version of an HTTP response.
     */
    private final HttpVersion myVersion;

    /**
     * The status code of an HTTP response.
     */
    private final int myStatusCode;

    /**
     * The status message of an HTTP response.
     */
    private final String myStatusMessage;

    /**
     * The headers of an HTTP response.
     */
    private final MultiMap myHeaders;

    /**
     * The trailers of an HTTP response.
     */
    private final MultiMap myTrailers;

    /**
     * The cookies of an HTTP response.
     */
    private final List<String> myCookies;

    /**
     * The body of an HTTP response.
     */
    private final String myBody;

    /**
     * The followed redirects of an HTTP response.
     */
    private final List<String> myRedirects;

    /**
     * Creates an object that embodies an HTTP response.
     *
     * @param aVersion An HTTP protocol version
     * @param aStatusCode An HTTP status code
     * @param aStatusMessage An HTTP status message
     * @param aHeaders A set of HTTP headers
     * @param aTrailers A set of HTTP trailers
     * @param aCookies A set of HTTP cookies
     * @param aBody A body of an HTTP response
     * @param aRedirects A set of followed redirects of an HTTP response
     */
    public HttpResponseImpl(final HttpVersion aVersion, final int aStatusCode, final String aStatusMessage,
            final MultiMap aHeaders, final MultiMap aTrailers, final List<String> aCookies, final String aBody,
            final List<String> aRedirects) {
        myVersion = aVersion;
        myStatusCode = aStatusCode;
        myStatusMessage = aStatusMessage;
        myHeaders = aHeaders;
        myTrailers = aTrailers;
        myCookies = aCookies;
        myBody = aBody;
        myRedirects = aRedirects;
    }

    @Override
    public HttpVersion version() {
        return myVersion;
    }

    @Override
    public int statusCode() {
        return myStatusCode;
    }

    @Override
    public String statusMessage() {
        return myStatusMessage;
    }

    @Override
    public String getHeader(final String aHeaderName) {
        return myHeaders.get(aHeaderName);
    }

    @Override
    public MultiMap trailers() {
        return myTrailers;
    }

    @Override
    public String getTrailer(final String aTrailerName) {
        return myTrailers.get(aTrailerName);
    }

    @Override
    public List<String> cookies() {
        return myCookies;
    }

    @Override
    public MultiMap headers() {
        return myHeaders;
    }

    @Override
    public String body() {
        return myBody;
    }

    @Override
    public Buffer bodyAsBuffer() {
        return Buffer.buffer(myBody);
    }

    @Override
    public List<String> followedRedirects() {
        return myRedirects;
    }

    @Override
    public JsonArray bodyAsJsonArray() {
        final Object value = Json.decodeValue(myBody);
        if (value instanceof JsonArray) {
            return (JsonArray) value;
        } else {
            throw new DecodeException(LOGGER.getMessage(MessageCodes.LCP_008, value.getClass().getName()));
        }
    }

}
