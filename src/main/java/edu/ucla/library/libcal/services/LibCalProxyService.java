
package edu.ucla.library.libcal.services;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * A service for retrieving config and calling LibCal APIs
 */
@ProxyGen
@VertxGen
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public interface LibCalProxyService {

    /**
     * The event bus address that the service will be registered on, for access via service proxies.
     */
    String ADDRESS = LibCalProxyService.class.getName();

    /**
     * Creates an instance of the service proxy.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig Application config in Json format
     * @return A Future that resolves to a service instance
     */
    static Future<LibCalProxyService> create(Vertx aVertx, JsonObject aConfig) {
        return Future.succeededFuture(new LibCalProxyServiceImpl(aVertx, aConfig));
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
     * Retrieves the output of a LibCal API call. FYI: PMD wants a container rather than the multiple String paramss,
     * but IMO the named params make the method call clearer
     *
     * @param anOAuthToken An OAuth bearer token
     * @param aQuery The query string passes to the LibCal API
     * @param aMethod The HTTP method used to contact LibCal
     * @param aBody The (possibly empty) request payload from the client
     * @return A Future that resolves to the HTTP response from LibCal represented as a JsonObject
     */
    Future<JsonObject> getLibCalOutput(String anOAuthToken, String aQuery, String aMethod, String aBody);

}
