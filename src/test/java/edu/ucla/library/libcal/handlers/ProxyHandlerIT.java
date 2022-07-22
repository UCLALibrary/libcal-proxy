
package edu.ucla.library.libcal.handlers;

import static edu.ucla.library.libcal.MediaType.APPLICATION_JSON;
import static info.freelibrary.util.Constants.INADDR_ANY;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.MessageCodes;
import edu.ucla.library.libcal.MediaType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link ProxyHandler#handle}.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class ProxyHandlerIT {

    /**
     * The test's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHandlerIT.class, MessageCodes.BUNDLE);

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
        final HttpRequest<Buffer> getOutput =
                myWebClient.get(myPort, INADDR_ANY, REQUEST_PATH).putHeader(Constants.X_FORWARDED_FOR, GOOD_FORWARDS);
        getOutput.send(result -> {
            if (result.succeeded()) {
                final HttpResponse<Buffer> response = result.result();

                assertEquals(HTTP.OK, response.statusCode());
                assertTrue(response.body().toString().contains("Powell Library"));
                aContext.completeNow();
            } else {
                aContext.failNow(result.cause());
            }
        });
    }

    /**
     * Tests that a client can get LibCap API output via POST.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testPostOutput(final Vertx aVertx, final VertxTestContext aContext) {
        final String jsonSource = "src/test/resources/json/register.json";
        final JsonObject payload = new JsonObject(aVertx.fileSystem().readFileBlocking(jsonSource));

        final HttpRequest<Buffer> postOutput =
                myWebClient.post(myPort, INADDR_ANY, POST_PATH).putHeader(Constants.X_FORWARDED_FOR, GOOD_FORWARDS)
                        .putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString());
        postOutput.sendJsonObject(payload, result -> {
            if (result.succeeded()) {
                final HttpResponse<Buffer> response = result.result();

                assertEquals(HTTP.OK, response.statusCode());
                assertTrue(response.body().toString().contains("booking_id"));
                aContext.completeNow();
            } else {
                aContext.failNow(result.cause());
            }
        });
    }

    /**
     * Tests that proxy handles bad POST request--this case, missing fields.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testBadPostOutput(final Vertx aVertx, final VertxTestContext aContext) {
        final String jsonSource = "src/test/resources/json/bad_register.json";
        final JsonObject payload = new JsonObject(aVertx.fileSystem().readFileBlocking(jsonSource));

        final HttpRequest<Buffer> postOutput =
                myWebClient.post(myPort, INADDR_ANY, POST_PATH).putHeader(Constants.X_FORWARDED_FOR, GOOD_FORWARDS)
                        .putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())
                        .expect(ResponsePredicate.SC_BAD_REQUEST);
        postOutput.sendJsonObject(payload, result -> {
            if (result.succeeded()) {
                final HttpResponse<Buffer> response = result.result();

                assertEquals(HTTP.BAD_REQUEST, response.statusCode());
                assertTrue(response.body().toString().contains("incomplete required"));
                aContext.completeNow();
            } else {
                aContext.failNow(result.cause());
            }
        });
    }

    /**
     * Tests that a client handles bad input
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testBadRequest(final Vertx aVertx, final VertxTestContext aContext) {
        final String badRequestPath = "/1.1/hours/2572";

        final HttpRequest<Buffer> getOutput =
                myWebClient.get(myPort, INADDR_ANY, badRequestPath).putHeader(Constants.X_FORWARDED_FOR, GOOD_FORWARDS);
        getOutput.send(result -> {
            if (result.succeeded()) {
                final HttpResponse<Buffer> response = result.result();

                assertEquals(HTTP.NOT_FOUND, response.statusCode());
                assertTrue(response.headers().get(HttpHeaders.CONTENT_TYPE).contains(MediaType.TEXT_HTML.toString()));

                aContext.completeNow();
            } else {
                aContext.failNow(result.cause());
            }
        });
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
        final HttpRequest<Buffer> getOutput =
                myWebClient.get(myPort, INADDR_ANY, aPath).putHeader(Constants.X_FORWARDED_FOR, GOOD_FORWARDS);
        getOutput.send(result -> {
            if (result.succeeded()) {
                final HttpResponse<Buffer> response = result.result();

                assertEquals(HTTP.OK, response.statusCode());
                assertTrue(response.headers().get(HttpHeaders.CONTENT_TYPE).contains(MediaType.TEXT_HTML.toString()));

                aContext.completeNow();
            } else {
                aContext.failNow(result.cause());
            }
        });
    }

    /**
     * Tests that proxy handler blocks bad IPs.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testBadClientIP(final Vertx aVertx, final VertxTestContext aContext) {
        final String badForward = "127.1.0.1,10.10.10.4";
        final HttpRequest<Buffer> getOutput =
                myWebClient.get(myPort, INADDR_ANY, REQUEST_PATH).putHeader(Constants.X_FORWARDED_FOR, badForward);

        getOutput.send(result -> {
            if (result.succeeded()) {
                final HttpResponse<Buffer> response = result.result();

                assertEquals(HTTP.FORBIDDEN, response.statusCode());
                assertTrue(response.body().toString().contains("unauthorized"));
                aContext.completeNow();
            } else {
                aContext.failNow(result.cause());
            }
        });
    }
}
