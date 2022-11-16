
package edu.ucla.library.libcal.handlers;

import static edu.ucla.library.libcal.MediaType.APPLICATION_JSON;
import static info.freelibrary.util.Constants.INADDR_ANY;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import info.freelibrary.util.HTTP;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.MediaType;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link ProxyHandler#handle}.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class ProxyHandlerIT {

    /**
     * The fake client and proxy IPs in X-FORWARDED header.
     */
    private static String GOOD_FORWARDS = "127.0.0.1,123.456.789.0";

    /**
     * A legit LibCal API call
     */
    private static String REQUEST_PATH = "/api/1.1/hours/2572";

    /**
     * A legit LibCal API call via POST
     */
    private static String POST_PATH = "/api/1.1/events/9353038/register";

    /**
     * A WebClient for calling the HTTP API.
     */
    protected WebClient myWebClient;

    /**
     * The port on which the application is listening.
     */
    protected int myPort;

    /**
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeAll
    public void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        ConfigRetriever.create(aVertx).getConfig().compose(config -> {
            myWebClient = WebClient.create(aVertx);
            myPort = config.getInteger(Config.HTTP_PORT, 8888);

            return Future.succeededFuture();
        }).onSuccess(result -> aContext.completeNow()).onFailure(aContext::failNow);
    }

    /**
     * Tests that a client can get LibCap API output.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetOutput(final Vertx aVertx, final VertxTestContext aContext) {
        final HttpRequest<Buffer> request = myWebClient.get(myPort, INADDR_ANY, REQUEST_PATH);

        request.putHeader(Constants.X_FORWARDED_FOR, GOOD_FORWARDS).send().onSuccess(response -> {
            aContext.verify(() -> {
                assertEquals(HTTP.OK, response.statusCode());
                assertTrue(response.body().toString().contains("Powell Library"));
            }).completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a client can get LibCap API output via POST.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testPostOutput(final Vertx aVertx, final VertxTestContext aContext) {
        final Buffer jsonBuffer = aVertx.fileSystem().readFileBlocking("src/test/resources/json/register.json");
        final HttpRequest<Buffer> request = myWebClient.post(myPort, INADDR_ANY, POST_PATH);

        request.putHeader(Constants.X_FORWARDED_FOR, GOOD_FORWARDS);
        request.putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString());

        request.sendJsonObject(new JsonObject(jsonBuffer)).onSuccess(response -> {
            aContext.verify(() -> {
                assertEquals(HTTP.OK, response.statusCode());
                assertTrue(response.body().toString().contains("booking_id"));
            }).completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that proxy handles bad POST request--this case, missing fields.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testBadPostOutput(final Vertx aVertx, final VertxTestContext aContext) {
        final Buffer jsonBuffer = aVertx.fileSystem().readFileBlocking("src/test/resources/json/bad_register.json");
        final HttpRequest<Buffer> request = myWebClient.post(myPort, INADDR_ANY, POST_PATH);

        request.putHeader(Constants.X_FORWARDED_FOR, GOOD_FORWARDS);
        request.putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString());

        request.sendJsonObject(new JsonObject(jsonBuffer)).onSuccess(response -> {
            aContext.verify(() -> {
                assertEquals(HTTP.BAD_REQUEST, response.statusCode());
                assertTrue(response.body().toString().contains("incomplete required"));
            }).completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a client handles bad input
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testBadRequest(final Vertx aVertx, final VertxTestContext aContext) {
        final HttpRequest<Buffer> request = myWebClient.get(myPort, INADDR_ANY, "/1.1/hours/2572"); // Bad path

        request.putHeader(Constants.X_FORWARDED_FOR, GOOD_FORWARDS).send().onSuccess(response -> {
            final MultiMap headers = response.headers();

            aContext.verify(() -> {
                assertEquals(HTTP.NOT_FOUND, response.statusCode());
                assertTrue(headers.get(HttpHeaders.CONTENT_TYPE).contains(MediaType.TEXT_HTML.toString()));
            }).completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that a client can make requests to paths that aren't part of the LibCal API, but are still valid
     * application routes.
     *
     * @param aPath The request path
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @ParameterizedTest
    @ValueSource(strings = { "/", "/admin/home" })
    public void testNonApiEndpointPath(final String aPath, final Vertx aVertx, final VertxTestContext aContext) {
        final HttpRequest<Buffer> request = myWebClient.get(myPort, INADDR_ANY, aPath);

        request.putHeader(Constants.X_FORWARDED_FOR, GOOD_FORWARDS).send().onSuccess(response -> {
            final MultiMap headers = response.headers();

            aContext.verify(() -> {
                assertEquals(HTTP.OK, response.statusCode());
                assertTrue(headers.get(HttpHeaders.CONTENT_TYPE).contains(MediaType.TEXT_HTML.toString()));
            }).completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that proxy handler blocks bad IPs.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testBadClientIP(final Vertx aVertx, final VertxTestContext aContext) {
        final HttpRequest<Buffer> request = myWebClient.get(myPort, INADDR_ANY, REQUEST_PATH);

        // Send with bad IP in X_FORWARDED_FOR
        request.putHeader(Constants.X_FORWARDED_FOR, "127.1.0.1,10.10.10.4").send().onSuccess(response -> {
            aContext.verify(() -> {
                assertEquals(HTTP.FORBIDDEN, response.statusCode());
                assertTrue(response.body().toString().contains("unauthorized"));
            }).completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that the proxy handler does not IP limit on open event registration endpoint.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testRegisteringWithClientIP(final Vertx aVertx, final VertxTestContext aContext) {
        final HttpRequest<Buffer> request = myWebClient.post(myPort, INADDR_ANY, "/api/1.1/events/9383207/register");
        final JsonObject payload = new JsonObject();
        final JsonObject formData = new JsonObject();

        // Populate registration form data
        formData.put("first_name", "Services");
        formData.put("last_name", "Test");
        formData.put("email", "softwaredev-services@library.ucla.edu");
        formData.put("q3", "Staff");

        // Set required POST parameters + form data
        payload.put("registration_type", "in-person");
        payload.put("form", formData);
        payload.put("no_email", 1);

        // Send with out of range IP in X_FORWARDED_FOR but it should still work
        request.putHeader(Constants.X_FORWARDED_FOR, "192.168.5.5").sendJson(payload).onSuccess(response -> {
            aContext.verify(() -> {
                assertEquals(HTTP.OK, response.statusCode(), response.bodyAsJsonObject().getString("error"));
            }).completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that the proxy handler does not IP limit on open events endpoint.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGettingEventsWithClientIP(final Vertx aVertx, final VertxTestContext aContext) {
        final HttpRequest<Buffer> request = myWebClient.get(myPort, INADDR_ANY, "/api/1.1/events/form/5481");

        // Send with out of range IP in X_FORWARDED_FOR but it should still work
        request.putHeader(Constants.X_FORWARDED_FOR, "192.168.5.6").send().onSuccess(response -> {
            aContext.verify(() -> {
                assertEquals(HTTP.OK, response.statusCode(), response.statusMessage());
                assertEquals("5481", response.bodyAsJsonArray().getJsonObject(0).getString("id"));
            }).completeNow();
        }).onFailure(aContext::failNow);
    }
}
