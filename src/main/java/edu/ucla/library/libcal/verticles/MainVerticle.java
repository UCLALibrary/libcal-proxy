
package edu.ucla.library.libcal.verticles;

import static info.freelibrary.util.Constants.INADDR_ANY;

import java.io.File;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.JsonKeys;
import edu.ucla.library.libcal.MessageCodes;
import edu.ucla.library.libcal.Op;
import edu.ucla.library.libcal.handlers.StatusHandler;
import edu.ucla.library.libcal.utils.TokenUtils;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.web.openapi.RouterBuilder;

/**
 * Main verticle that starts the application.
 */
public class MainVerticle extends AbstractVerticle {

    /**
     * A logger for the main verticle.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class, MessageCodes.BUNDLE);

    /**
     * An OpenAPI definition that the main verticle users to route requests.
     */
    private static final String API_SPEC = "src/main/resources/libcal-proxy.yaml";

    /**
     * The main verticle's HTTP server.
     */
    private HttpServer myServer;

    /**
     * LibCal credentials represented as an OAuth token.
     */
    private User myOAuthToken;

    /**
     * The ID of the periodic timer for checking if the OAuth token needs refreshing.
     */
    private long myTimerId;

    @Override
    public void start(final Promise<Void> aPromise) {
        ConfigRetriever.create(vertx).getConfig().compose(config -> {
            final OAuth2Auth oauth2 = getOAuth2Provider(config);

            return oauth2.authenticate(new JsonObject()).compose(token -> {
                LOGGER.debug(MessageCodes.LCP_002, config.getString(Config.OAUTH_CLIENT_ID),
                        token.principal().encodePrettily());

                myOAuthToken = token;
                myTimerId = vertx.setPeriodic(1000, refreshTokenIfExpired(oauth2));

                shareAccessToken(token);

                return configureServer(config);
            });
        }).onSuccess(server -> {
            LOGGER.info(MessageCodes.LCP_001, server.actualPort());
            aPromise.complete();
        }).onFailure(aPromise::fail);
    }

    @Override
    public void stop(final Promise<Void> aPromise) {
        myServer.close().map(unused -> vertx.cancelTimer(myTimerId)) //
                .onFailure(aPromise::fail) //
                .onSuccess(unused -> aPromise.complete());
    }

    /**
     * Gets an OAuth2 authentication provider.
     *
     * @param aConfig A JSON configuration
     * @return The provider
     */
    private OAuth2Auth getOAuth2Provider(final JsonObject aConfig) {
        final OAuth2Options options = new OAuth2Options().setFlow(OAuth2FlowType.CLIENT)
                .setClientId(aConfig.getString(Config.OAUTH_CLIENT_ID))
                .setClientSecret(aConfig.getString(Config.OAUTH_CLIENT_SECRET))
                .setSite(aConfig.getString(Config.OAUTH_TOKEN_URL));

        return OAuth2Auth.create(vertx, options);
    }

    /**
     * Creates a {@link Handler} that checks if the OAuth token has expired and if so, refreshes it.
     *
     * @param anOAuth2Provider An OAuth2 provider
     * @return A handler that performs the action
     */
    private Handler<Long> refreshTokenIfExpired(final OAuth2Auth anOAuth2Provider) {
        return unused -> {
            if (myOAuthToken.expired()) {
                anOAuth2Provider.refresh(myOAuthToken).onSuccess(newUser -> {
                    myOAuthToken = newUser;

                    shareAccessToken(newUser);

                    LOGGER.debug("OAuth token refreshed");
                }).onFailure(details -> {
                    // FIXME: https://vertx.io/docs/vertx-auth-oauth2/java/#_refresh_token
                });
            }
        };
    }

    /**
     * Configure the application server.
     *
     * @param aConfig A JSON configuration
     * @return A Future that resolves to the configured and listening server
     */
    private Future<HttpServer> configureServer(final JsonObject aConfig) {
        final String host = aConfig.getString(Config.HTTP_HOST, INADDR_ANY);
        final int port = aConfig.getInteger(Config.HTTP_PORT, 8888);

        return RouterBuilder.create(vertx, getRouterSpec()).compose(routeBuilder -> {
            final HttpServerOptions serverOptions = new HttpServerOptions().setPort(port).setHost(host);

            // Associate handlers with operation IDs from the application's OpenAPI specification
            routeBuilder.operation(Op.GET_STATUS).handler(new StatusHandler(getVertx()));

            myServer = getVertx().createHttpServer(serverOptions).requestHandler(routeBuilder.createRouter());

            return myServer.listen();
        });
    }

    /**
     * Gets the OpenAPI specification used to configure the application's router. If the file doesn't exist on the file
     * system, we assume we're running from a Jar and that it can be found in the classpath.
     *
     * @return The OpenAPI router specification
     */
    private String getRouterSpec() {
        final File specFile = new File(API_SPEC);
        return specFile.exists() ? API_SPEC : specFile.getName();
    }

    /**
     * Make the access token accessible to the rest of the application via {@link TokenUtils#getAccessToken}.
     *
     * @param anOAuthToken An OAuth token
     */
    private void shareAccessToken(final User anOAuthToken) {
        vertx.sharedData().getLocalMap(Constants.ACCESS_TOKEN_MAP).put(Constants.ACCESS_TOKEN,
                anOAuthToken.principal().getString(JsonKeys.ACCESS_TOKEN));
    }

    /**
     * Starts up the main verticle.
     *
     * @param aArgsArray An array of arguments
     */
    @SuppressWarnings("UncommentedMain")
    public static void main(final String[] aArgsArray) {
        Vertx.vertx().deployVerticle(new MainVerticle());
    }
}
