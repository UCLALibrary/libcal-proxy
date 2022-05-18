
package edu.ucla.library.libcal.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.ucla.library.libcal.MessageCodes;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.ext.web.client.HttpResponse;
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
        myWebClient = WebClient.create(aVertx);

        ConfigRetriever.create(aVertx).getConfig().compose(config -> {
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
        final String token = OAuthTokenService.getBearerToken(aVertx);

        assertTrue(token != null);
        LOGGER.debug(MessageCodes.LCP_003, token);

        exampleApiCall(token).onSuccess(response -> {
            assertEquals(200, response.statusCode());

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that {@link OAuthTokenService#refreshTokenIfExpired()} behaves as expected.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public final void testRefreshTokenIfExpired(final Vertx aVertx, final VertxTestContext aContext) {
        final String tokenBeforeRefresh = OAuthTokenService.getBearerToken(aVertx);

        try {
            final long delaySeconds = 3600;

            LOGGER.debug("Waiting until {} to try refreshing the token...",
                    ZonedDateTime.now().plusSeconds(delaySeconds).format(DateTimeFormatter.RFC_1123_DATE_TIME));
            Thread.sleep(delaySeconds * 1000);
        } catch (final InterruptedException details) {
            aContext.failNow(details);
            return;
        }

        myServiceProxy.refreshTokenIfExpired().compose(unused -> {
            final String tokenAfterRefresh = OAuthTokenService.getBearerToken(aVertx);

            LOGGER.debug("Token before refresh: {}, after: {}", tokenBeforeRefresh, tokenAfterRefresh);

            return exampleApiCall(tokenAfterRefresh);
        }).onSuccess(response -> {
            assertEquals(200, response.statusCode());

            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Sends an example request to the LibCal API.
     *
     * @param aToken The bearer token
     * @return A Future that resolves to an HTTP response
     */
    private Future<HttpResponse<Buffer>> exampleApiCall(final String aToken) {
        return myWebClient.getAbs("https://calendar.library.ucla.edu/1.1/calendars").bearerTokenAuthentication(aToken)
                .send();
    }
}
