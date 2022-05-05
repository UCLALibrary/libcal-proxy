package edu.ucla.library.libcal.services;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * A service for handling OAuth token-related functions.
 */
@ProxyGen
@VertxGen
public interface LibCalProxyService {

    /**
     * The event bus address that the service will be registered on, for access via service proxies.
     */
    String ADDRESS = LibCalProxyService.class.getName();

    /**
     * Creates an instance of the service.
     *
     * @param aVertx A Vert.x instance
     * @return The service instance
     */
    static LibCalProxyService create(Vertx aVertx) {
        return new LibCalProxyServiceImpl(aVertx);
    }

    /**
     * Creates an instance of the service proxy. Note that the service itself must have already been instantiated with
     * {@link #create} in order for this method to succeed.
     *
     * @param aVertx A Vert.x instance
     * @return A service proxy instance
     */
    static LibCalProxyService createProxy(Vertx aVertx) {
        return new ServiceProxyBuilder(aVertx).setAddress(ADDRESS).build(LibCalProxyService.class);
    }

    /**
     * Retrieves application config.
     *
     * @return A Future that resolves to a JSON object of config values
     */
    Future<JsonObject> getConfig();

    /**
     * Retrieves an OAuth bearer (access) token.
     *
     * @param aClientID A LibCal client ID used in authentication
     * @param aClientSecret A LibCal client secret used in authentication
     * @param aTokenURL The LibCal OAuth token endpoint
     * @return A Future that resolves to a JSON object containing the token and its expiration date
     */
    Future<JsonObject> getAccessToken(String aClientID, String aClientSecret, String aTokenURL);

    /**
     * Retrieves the JSON content produced by a SpringShare LibCal API call
     *
     * @param aOAuthToken An OAuth bearer token used for API authorization
     * @param aBaseURL The base URL used for calls to UCLA instances of LibCal APIs
     * @param aQuery The API-specific query string used to get data from LibCal
     * @return A Future that resolves to a JSON object of LibCal data
     */
    Future<JsonObject> getLibCalOutput(String aOAuthToken, String aBaseURL, String aQuery);

}
