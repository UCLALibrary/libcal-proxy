
package edu.ucla.library.libcal.services;

import edu.ucla.library.libcal.Config;

import io.vertx.codegen.annotations.ProxyClose;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * A service for managing the OAuth token used by LibCal API clients.
 */
@ProxyGen
@VertxGen
public interface OAuthTokenService {

    /**
     * The event bus address that the service will be registered on, for access via service proxies.
     */
    String ADDRESS = OAuthTokenService.class.getName();

    /**
     * Creates an instance of the service.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig A configuration
     * @return The service instance
     */
    static Future<OAuthTokenService> create(final Vertx aVertx, final JsonObject aConfig) {
        final OAuth2Options options = new OAuth2Options().setFlow(OAuth2FlowType.CLIENT)
                .setClientId(aConfig.getString(Config.OAUTH_CLIENT_ID))
                .setClientSecret(aConfig.getString(Config.OAUTH_CLIENT_SECRET))
                .setSite(aConfig.getString(Config.OAUTH_TOKEN_URL));
        final OAuth2Auth provider = OAuth2Auth.create(aVertx, options);

        return provider.authenticate(new JsonObject()).compose(token -> {
            final OAuthTokenService service = new OAuthTokenServiceImpl(aVertx, aConfig, provider, token);

            return ((OAuthTokenServiceImpl) service).shareAccessToken(token).map(service);
        });
    }

    /**
     * Creates an instance of the service proxy.
     *
     * @param aVertx A Vert.x instance
     * @return A service proxy instance
     */
    static OAuthTokenService createProxy(final Vertx aVertx) {
        return new ServiceProxyBuilder(aVertx).setAddress(ADDRESS).build(OAuthTokenService.class);
    }

    /**
     * Gets the bearer token that LibCal API clients should send in the "Authorization" HTTP header.
     *
     * @return A Future that resolves to the value of the bearer token
     */
    Future<String> getBearerToken();

    /**
     * Closes the underlying resources used by this service.
     *
     * @return A Future that resolves once the resources have been closed
     */
    @ProxyClose
    Future<Void> close();
}
