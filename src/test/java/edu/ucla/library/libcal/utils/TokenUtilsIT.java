
package edu.ucla.library.libcal.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.ucla.library.libcal.verticles.MainVerticle;
import edu.ucla.library.libcal.MessageCodes;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests related to the TokenUtils class.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class TokenUtilsIT {

    /**
     * The logger used by these tests.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenUtilsIT.class, MessageCodes.BUNDLE);

    /**
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeAll
    public final void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        aVertx.deployVerticle(new MainVerticle()).onSuccess(deploymentID -> {
            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tears down the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @AfterAll
    public final void tearDown(final Vertx aVertx, final VertxTestContext aContext) {
        final Stream<Future<Void>> undeployAll = aVertx.deploymentIDs().stream().map(id -> aVertx.undeploy(id));

        CompositeFuture.all(undeployAll.collect(Collectors.toList())).onSuccess(result -> {
            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests the <code>getAccessToken()</code> method.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public final void testGetAccessToken(final Vertx aVertx, final VertxTestContext aContext) {
        final String token = TokenUtils.getAccessToken(aVertx);

        assertTrue(token != null);
        LOGGER.debug(MessageCodes.LCP_003, token);
        aContext.completeNow();
    }
}
