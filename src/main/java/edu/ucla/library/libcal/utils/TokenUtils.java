package edu.ucla.library.libcal.utils;

import edu.ucla.library.libcal.JsonKeys;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;

/**
 * Utility method for retrieving OAuth access tokens.
*/
public class TokenUtils {

    /**
     * Private constructor for TokenUtils class.
     */
    private TokenUtils() {
    }

    /**
      * Retrieves an access token from LibCal OAuth provider.
      *
      * @param aClientInfo JSON object holding client ID and secret
      * @param aVertx A Vertx object used to build token request
      * @return JSON object wrapping the access token
    */
    public static JsonObject getAccessToken(final JsonObject aClientInfo, final Vertx aVertx) {
      final JsonObject token = new JsonObject();
      final Future<String> rawToken = handleRawToken(aClientInfo, aVertx);

      rawToken.onComplete(result->{
        token.put(JsonKeys.ACCESS_TOKEN, rawToken.result());
      });

      return token;
    }

    private static Future<String> handleRawToken(final JsonObject aClientInfo, final Vertx aVertx) {
      final Promise<String> promise = Promise.promise();

      OAuth2Options credentials = new OAuth2Options()
        .setFlow(OAuth2FlowType.CLIENT)
        .setClientId(aClientInfo.getString(JsonKeys.CLIENT_ID))
        .setClientSecret(aClientInfo.getString(JsonKeys.CLIENT_SECRET))
        .setSite(aClientInfo.getString(JsonKeys.TOKEN_ENDPOINT));

      OAuth2Auth oauth2 = OAuth2Auth.create(aVertx, credentials);

      JsonObject tokenConfig = new JsonObject();
      oauth2.authenticate(tokenConfig)
        .onSuccess(user -> {
          promise.complete(user.get("access_token").toString());
        }).onFailure(err -> {
          promise.fail(err.getMessage());
      });
      return promise.future();
    }
}
