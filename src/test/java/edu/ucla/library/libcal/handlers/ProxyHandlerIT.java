
package edu.ucla.library.libcal.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static edu.ucla.library.libcal.MediaType.APPLICATION_JSON;

import info.freelibrary.util.HTTP;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.Constants;
import edu.ucla.library.libcal.JsonKeys;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.json.JsonObject;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link ProxyHandler#handle}.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class ProxyHandlerIT {

    /**
     * Tests that a client can get LibCap API output.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testGetOutput(final Vertx aVertx, final VertxTestContext aContext) {
        final String requestPath = "/libcal/api%2F1.1%2Fhours%2F2572";
        final WebClient webClient = WebClient.create(aVertx);
        final int port = 8888; //aConfig.get(Config.HTTP_PORT);

        webClient.get(port, Constants.UNSPECIFIED_HOST, requestPath).expect(ResponsePredicate.SC_SUCCESS)
            .as(BodyCodec.jsonObject()).send(result -> {
                if (result.succeeded()) {
                    final HttpResponse<JsonObject> response = result.result();

                    assertEquals(HTTP.OK, response.statusCode());
                    assertEquals("Powell Library", response.body().getValue(JsonKeys.NAME));
                    aContext.completeNow();
                } else {
                    aContext.failNow(result.cause());
                }
	    });

    }

}
