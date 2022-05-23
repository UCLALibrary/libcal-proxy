
package edu.ucla.library.libcal.services;

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
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * Tests for {@link LibCalProxyService}.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class LibCalProxyServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthTokenServiceIT.class, MessageCodes.BUNDLE);

    /**
     * The service proxy for getting bearer token for API call
     */
    private OAuthTokenService myTokenProxy;

    /**
     * The service proxy for testing typical client usage.
     */
    private LibCalProxyService myServiceProxy;

    /**
     * Only used for event bus unregistration.
     */
    private MessageConsumer<?> myToken;

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
        ConfigRetriever.create(aVertx).getConfig().compose(config -> {
            return OAuthTokenService.create(aVertx, config);
        }).onSuccess(service -> {
            myToken = new ServiceBinder(aVertx).setAddress(OAuthTokenService.ADDRESS).register(OAuthTokenService.class,
                    service);
            myService = new ServiceBinder(aVertx).setAddress(LibCalProxyService.ADDRESS)
                    .register(LibCalProxyService.class, myServiceProxy);
            myTokenProxy = OAuthTokenService.createProxy(aVertx);
            myServiceProxy = LibCalProxyService.createProxy(aVertx);

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
     * Tests that {@link LibCalProxyService#getConfig()} returns application config.
     *
     * @param aContext A test context
     */
    @Test
    public final void testGetConfig(final VertxTestContext aContext) {
        myServiceProxy.getConfig().compose(config -> {
            assertTrue(config != null);
            assertTrue(config.getString(Config.OAUTH_CLIENT_ID) != null);

            return Future.succeededFuture(null);
        }).onSuccess(result -> {
            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }

    /**
     * Tests that {@link OAuthTokenService#getBearerToken(Vertx)} returns a bearer token that can be used to authorize
     * LibCal API calls.
     *
     * @param aContext A test context
     */
    @Test
    public final void testGetLibCalOutput(final VertxTestContext aContext) {
        myTokenProxy.getBearerToken().compose(token -> {
            myServiceProxy.getLibCalOutput(token, "https://calendar.library.ucla.edu/", "1.1/calendars")
                    .compose(output -> {
                        assertTrue(output != null);
                        aContext.completeNow();
                        return Future.succeededFuture(null);
                    });
            aContext.completeNow();
            return Future.succeededFuture(null);
        }).onSuccess(result -> {
            aContext.completeNow();
        }).onFailure(aContext::failNow);
    }
}
