
package edu.ucla.library.libcal.verticles;

import static info.freelibrary.util.Constants.INADDR_ANY;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import info.freelibrary.util.HTTP;

import edu.ucla.library.libcal.Config;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests the main verticle of the Vert.x application.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class MainVerticleTest {

    /**
     * Sets up the test.
     *
     * @param aContext A test context
     */
    @BeforeAll
    public void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        final int port = Integer.parseInt(System.getenv(Config.HTTP_PORT));
        final DeploymentOptions options = new DeploymentOptions();

        options.setConfig(new JsonObject().put(Config.HTTP_PORT, port));

        aVertx.deployVerticle(MainVerticle.class.getName(), options).onSuccess(result -> aContext.completeNow())
                .onFailure(aContext::failNow);
    }

    /**
     * Tests the server can start successfully.
     *
     * @param aContext A test context
     */
    @Test
    public void testThatTheServerIsStarted(final Vertx aVertx, final VertxTestContext aContext) {
        final int port = Integer.parseInt(System.getenv(Config.HTTP_PORT));
        final WebClient client = WebClient.create(aVertx);

        client.get(port, INADDR_ANY, "/status").send().onSuccess(response -> {
            assertEquals(HTTP.OK, response.statusCode());
            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }
}
