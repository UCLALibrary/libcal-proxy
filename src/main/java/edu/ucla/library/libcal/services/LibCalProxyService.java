package edu.ucla.library.libcal.services;

import io.vertx.codegen.annotations.ProxyClose;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceProxyBuilder;

@ProxyGen
@VertxGen
public interface LibCalProxyService {

    String ADDRESS = LibCalProxyService.class.getName();

    /**
     * Creates an instance of the service proxy.
     *
     * @param aVertx A Vert.x instance
     * @return A service instance
     */
    static LibCalProxyService create(Vertx aVertx) {
        return new LibCalProxyServiceImpl(aVertx);
    }

    /**
     * Creates an instance of the service proxy.
     *
     * @param aVertx A Vert.x instance
     * @return A service proxy instance
     */
    static LibCalProxyService createProxy(Vertx aVertx) {
        return new ServiceProxyBuilder(aVertx).setAddress(ADDRESS).build(LibCalProxyService.class);
    }

    /**
     * Retrieves the application config.
     *
     * @return A Future that resolves to a config bundle
     */
    Future<JsonObject> getConfig();

    /**
     * Retrieves the output of a LibCal API call.
     *
     * @param aOUathToken An OAuth bearer token
     * @param aaBaseURL The base URL for calling UCLA LibCal APIs
     * @param aQuery The query string passes to the LibCal API
     * @return A Future that resolves to the JSON response from LibCal
     */
    Future<JsonObject> getLibCalOutput(final String aOUathToken, final String aBaseURL, final String aQuery);


    /**
     * Closes the underlying resources used by this service.
     *
     * @return A Future that resolves once the resources have been closed
     */
    @ProxyClose
    Future<Void> close();
}
