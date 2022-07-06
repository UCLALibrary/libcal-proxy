
package edu.ucla.library.libcal;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.HttpResponse;

import java.util.List;
import java.util.function.Function;

public class HttpResponseImpl<T> implements HttpResponse<T> {

    /**
     * Function variable to handle JsonArray decoding.
     */
    public static final Function<Buffer, JsonArray> JSON_ARRAY_DECODER = buff -> {
        final Object val = Json.decodeValue(buff);
        if (val == null) {
            return null;
        }
        if (val instanceof JsonArray) {
            return (JsonArray) val;
        }
        throw new DecodeException("Invalid Json Object decoded as " + val.getClass().getName());
    };

    /**
     * Function variable to handle JsonArray decoding.
     */
    private final HttpVersion myVersion;

    /**
     * Function variable to handle JsonArray decoding.
     */
    private final int statusCode;

    /**
     * Function variable to handle JsonArray decoding.
     */
    private final String statusMessage;

    /**
     * Function variable to handle JsonArray decoding.
     */
    private final MultiMap headers;

    /**
     * Function variable to handle JsonArray decoding.
     */
    private final MultiMap trailers;

    /**
     * Function variable to handle JsonArray decoding.
     */
    private final List<String> cookies;

    /**
     * Function variable to handle JsonArray decoding.
     */
    private final T body;

    /**
     * Function variable to handle JsonArray decoding.
     */
    private final List<String> redirects;

    public HttpResponseImpl(HttpVersion aVersion, int statusCode, String statusMessage, MultiMap headers,
            MultiMap trailers, List<String> cookies, T body, List<String> redirects) {
        myVersion = aVersion;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.headers = headers;
        this.trailers = trailers;
        this.cookies = cookies;
        this.body = body;
        this.redirects = redirects;
    }

    @Override
    public HttpVersion myVersion() {
        return myVersion;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public String statusMessage() {
        return statusMessage;
    }

    @Override
    public String getHeader(String headerName) {
        return headers.get(headerName);
    }

    @Override
    public MultiMap trailers() {
        return trailers;
    }

    @Override
    public String getTrailer(String trailerName) {
        return trailers.get(trailerName);
    }

    @Override
    public List<String> cookies() {
        return cookies;
    }

    @Override
    public MultiMap headers() {
        return headers;
    }

    @Override
    public T body() {
        return body;
    }

    @Override
    public Buffer bodyAsBuffer() {
        return body instanceof Buffer ? (Buffer) body : null;
    }

    @Override
    public List<String> followedRedirects() {
        return redirects;
    }

    @Override
    public JsonArray bodyAsJsonArray() {
        Buffer b = bodyAsBuffer();
        return b != null ? JSON_ARRAY_DECODER.apply(b) : null;
    }

}
