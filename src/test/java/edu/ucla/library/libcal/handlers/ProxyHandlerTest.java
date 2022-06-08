
package edu.ucla.library.libcal.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.freelibrary.util.HTTP;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.MessageCodes;
import edu.ucla.library.libcal.verticles.MainVerticle;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link ProxyHandler#handle}.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class ProxyHandlerTest {

    /**
     * The test's logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHandlerTest.class, MessageCodes.BUNDLE);

    /**
     * The default port that the application listens on.
     */
    private static String DEFAULT_PORT = "8888";

    /**
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeAll
    public void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        final int port = Integer.parseInt(System.getenv().getOrDefault(Config.HTTP_PORT, DEFAULT_PORT));
        final DeploymentOptions options = new DeploymentOptions();

        options.setConfig(new JsonObject().put(Config.HTTP_PORT, port));

        aVertx.deployVerticle(MainVerticle.class.getName(), options).onSuccess(result -> aContext.completeNow())
                .onFailure(aContext::failNow);
    }

    /**
     * Tests that a client can get LibCap API output.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetOutput(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestPath = "/libcal/api%2F1.1%2Fhours%2F2572";
        final WebClient webClient = WebClient.create(aVertx);
        final int port = Integer.parseInt(DEFAULT_PORT);

        webClient.get(port, Constants.LOCAL_HOST, requestPath).expect(ResponsePredicate.SC_SUCCESS)
            .as(BodyCodec.string()).send(result -> {
                if (result.succeeded()) {
                    final HttpResponse<String> response = result.result();

                    assertEquals(HTTP.OK, response.statusCode());
                    assertTrue(response.body().contains("Powell Library"));
                    aContext.completeNow();
                } else {
                    aContext.failNow(result.cause());
                }
            });
    }

}
