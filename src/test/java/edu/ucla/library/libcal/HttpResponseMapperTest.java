
package edu.ucla.library.libcal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests {@link HttpResponseMapper}.
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class HttpResponseMapperTest {

    /**
     * An HTTP client.
     */
    private WebClient myWebClient;

    /**
     * A response mapper.
     */
    private HttpResponseMapper myMapper;

    /**
     * Sets up the test.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @BeforeAll
    public void setUp(final Vertx aVertx, final VertxTestContext aContext) {
        myWebClient = WebClient.create(aVertx);
        myMapper = new HttpResponseMapper();

        aContext.completeNow();
    }

    /**
     * Tests that encode reverses decode.
     *
     * @param aVertx A Vert.x instance
     * @param aContext A test context
     */
    @Test
    public void testDecodeReversesEncode(final Vertx aVertx, final VertxTestContext aContext) {
        myWebClient.getAbs("http://example.com").as(BodyCodec.string()).send().onSuccess(response -> {
            aContext.verify(() -> {
                final HttpResponse<String> decodedEncodedResponse = myMapper.decode(myMapper.encode(response));

                assertEquals(response.statusCode(), decodedEncodedResponse.statusCode());
                assertEquals(response.statusMessage(), decodedEncodedResponse.statusMessage());
                assertEquals(response.headers().toString(), decodedEncodedResponse.headers().toString());
                assertEquals(response.body(), decodedEncodedResponse.body());
                assertEquals(response.trailers().toString(), decodedEncodedResponse.trailers().toString());
            }).completeNow();
        }).onFailure(aContext::failNow);
    }
}
