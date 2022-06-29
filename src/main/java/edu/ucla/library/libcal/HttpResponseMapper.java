
package edu.ucla.library.libcal;

import java.util.List;
import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.impl.HttpResponseImpl;

/**
 * A mapper that allows for sending and receiving {@link HttpResponse}s over the event bus.
 */
public class HttpResponseMapper {

    /**
     * The internal JSON key to associate with the HTTP protocol version of an HTTP response.
     */
    private static final String HTTP_VERSION = "http_version";

    /**
     * The internal JSON key to associate with the status code of an HTTP response.
     */
    private static final String STATUS_CODE = "status_code";

    /**
     * The internal JSON key to associate with the status message of an HTTP response.
     */
    private static final String STATUS_MESSAGE = "status_message";

    /**
     * The internal JSON key to associate with the headers of an HTTP response.
     */
    private static final String HEADERS = "headers";

    /**
     * The internal JSON key to associate with the trailers of an HTTP response.
     */
    private static final String TRAILERS = "trailers";

    /**
     * The internal JSON key to associate with the cookies of an HTTP response.
     */
    private static final String COOKIES = "cookies";

    /**
     * The internal JSON key to associate with the body of an HTTP response.
     */
    private static final String BODY = "body";

    /**
     * The internal JSON key to associate with the followed redirects of an HTTP response.
     */
    private static final String FOLLOWED_REDIRECTS = "followed_redirects";

    /**
     * Represents an {@HttpResponse} as a {@JsonObject}.
     *
     * @param aResponse The HTTP response
     * @return The JsonObject representation
     */
    public JsonObject encode(final HttpResponse<String> aResponse) {
        return new JsonObject() //
                .put(HTTP_VERSION, aResponse.version()) //
                .put(STATUS_CODE, aResponse.statusCode()) //
                .put(STATUS_MESSAGE, aResponse.statusMessage()) //
                .put(HEADERS, jsonArrayfromMultiMap(aResponse.headers())) //
                .put(TRAILERS, jsonArrayfromMultiMap(aResponse.trailers())) //
                .put(COOKIES, aResponse.cookies()) //
                .put(BODY, aResponse.body()) //
                .put(FOLLOWED_REDIRECTS, aResponse.followedRedirects());
    }

    /**
     * Represents a {@link JsonObject} as an {@link HttpResponse}.
     *
     * @param aJsonObject The JSON object
     * @return The HttpResponse representation
     */
    @SuppressWarnings("unchecked")
    public HttpResponse<String> decode(final JsonObject aJsonObject) {
        return new HttpResponseImpl<String>( //
                HttpVersion.valueOf(aJsonObject.getString(HTTP_VERSION)), //
                aJsonObject.getInteger(STATUS_CODE).intValue(), //
                aJsonObject.getString(STATUS_MESSAGE), //
                multiMapfromJsonArray(aJsonObject.getJsonArray(HEADERS)), //
                multiMapfromJsonArray(aJsonObject.getJsonArray(TRAILERS)), //
                (List<String>) aJsonObject.getJsonArray(COOKIES).getList(), //
                aJsonObject.getString(BODY), //
                (List<String>) aJsonObject.getJsonArray(FOLLOWED_REDIRECTS).getList());
    }

    /**
     * Represents a {@link MultiMap} as a {@link JsonArray}.
     *
     * @param aMultiMap The map
     * @return The JsonArray representation
     */
    private static JsonArray jsonArrayfromMultiMap(final MultiMap aMultiMap) {
        final JsonArray array = new JsonArray();

        aMultiMap.forEach((entryKey, entryValue) -> {
            array.add(new JsonObject().put(entryKey, entryValue));
        });

        return array;
    }

    /**
     * Represents a {@link JsonArray} as a {@link MultiMap}.
     *
     * @param aJsonArray The array
     * @return The MultiMap representation
     */
    private static MultiMap multiMapfromJsonArray(final JsonArray aJsonArray) {
        final MultiMap map = MultiMap.caseInsensitiveMultiMap();

        aJsonArray.forEach(jsonObject -> {
            final Map<String, Object> underlyingMap = ((JsonObject) jsonObject).getMap();
            final Map.Entry<String, Object> entry = underlyingMap.entrySet().iterator().next();

            map.add(entry.getKey(), (String) entry.getValue());
        });

        return map;
    }
}
