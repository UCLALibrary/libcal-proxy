
package edu.ucla.library.libcal.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.MessageCodes;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * Tests for {@link OAuthTokenService}.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class OAuthTokenServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthTokenServiceIT.class, MessageCodes.BUNDLE);

    /**
     * HTTP client for testing the OAuth token.
     */
    private WebClient myWebClient;

    /**
     * The service proxy for testing typical client usage.
     */
    private OAuthTokenService myServiceProxy;

    /**
     * Only used for event bus unregistration.
     */
    private MessageConsumer<?> myService;

    /**
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeAll
    public final void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        final ConfigRetriever cr = ConfigRetriever.create(aVertx).setConfigurationProcessor(Config::removeEmptyString);

        myWebClient = WebClient.create(aVertx);

        cr.getConfig().compose(config -> {
            return OAuthTokenService.create(aVertx, config);
        }).onSuccess(service -> {
            myService = new ServiceBinder(aVertx).setAddress(OAuthTokenService.ADDRESS)
                    .register(OAuthTokenService.class, service);
            myServiceProxy = OAuthTokenService.createProxy(aVertx);

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
        myServiceProxy.close().compose(result -> myService.unregister()).onSuccess(success -> aContext.completeNow())
                .onFailure(aContext::failNow);
    }

    /**
     * Tests that {@link OAuthTokenService#getBearerToken(Vertx)} returns a bearer token that can be used to authorize
     * LibCal API calls.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public final void testGetToken(final Vertx aVertx, final VertxTestContext aContext) {
        myServiceProxy.getBearerToken().compose(token -> {
            final HttpRequest<?> exampleApiRequest = myWebClient
                    .getAbs("https://calendar.library.ucla.edu/api/1.1/hours/2572").bearerTokenAuthentication(token);

            assertTrue(token != null);
            LOGGER.debug(MessageCodes.LCP_003, token);

            return exampleApiRequest.send();
        }).onSuccess(response -> {
            assertEquals(200, response.statusCode());

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }
}
