package edu.ucla.library.libcal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;

import edu.ucla.library.libcal.Config;
import edu.ucla.library.libcal.JsonKeys;

public class TokenUtilsTest {
    
    @Test
    public final void testGetToken() {
        final Vertx vertx = Vertx.vertx();
        final ConfigStoreOptions envPropsStore = new ConfigStoreOptions().setType("env");
        final ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys");
        final ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(envPropsStore).addStore(sysPropsStore);
        final ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        retriever.getConfig(ar -> {
           if (ar.failed()) {
             System.err.println(ar.cause().getMessage());
           } else {
             final JsonObject config = ar.result();
             final JsonObject clientInfo = new JsonObject()
                   .put(JsonKeys.CLIENT_ID, config.getString(Config.OAUTH_CLIENT_ID))
                   .put(JsonKeys.CLIENT_SECRET, config.getString(Config.OAUTH_CLIENT_SECRET))
                   .put(JsonKeys.TOKEN_ENDPOINT, config.getString(Config.OAUTH_TOKEN_URL));
             final JsonObject accessToken = TokenUtils.getAccessToken(clientInfo,vertx);
	     System.out.println("token : " + accessToken.encodePrettily());
             assertTrue(accessToken.containsKey(JsonKeys.ACCESS_TOKEN));
           }      
        });
    }
}
