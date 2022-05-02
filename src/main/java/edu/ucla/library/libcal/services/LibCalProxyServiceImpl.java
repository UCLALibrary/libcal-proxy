package edu.ucla.library.libcal.services;

import edu.ucla.library.libcal.JsonKeys;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;

public class LibCalProxyServiceImpl implements LibCalProxyService {

    private final Vertx myVertx;

    LibCalProxyServiceImpl(final Vertx aVertx) {
        myVertx = aVertx;
    }

    @Override
    public Future<JsonObject> getClientCredentials(final String aAppName, final JsonObject aConfig) {
	final JsonObject credentials = new JsonObject();
        credentials.put(JsonKeys.CLIENT_ID, aConfig.getString(JsonKeys.CLIENT_ID.concat(aAppName)));
        credentials.put(JsonKeys.CLIENT_SECRET, aConfig.getString(JsonKeys.CLIENT_SECRET.concat(aAppName)));
        return Future.succeededFuture(credentials);
    }

    @Override
    public Future<JsonObject> getAccessToken(final String aClientID, final String aClientSecret) {
	/* add call to TokenUtils here once code finalized */
        return null;
    }

    @Override
    public Future<JsonObject> getLibCalOutput(final String aOUathToken, final String aBaseURL, final String aQuery) {
        final Promise<JsonObject> promise = Promise.promise();
	final HttpRequest<JsonObject> request;
        final JsonObject responseBody = new JsonObject();

        request = WebClient.create(myVertx).getAbs(aBaseURL.concat(aQuery))
                .bearerTokenAuthentication(aOUathToken)
                .as(BodyCodec.jsonObject()).expect(ResponsePredicate.SC_OK).ssl(true);
        request.send(asyncResult -> {
          if (asyncResult.succeeded()) {
                responseBody.mergeIn(asyncResult.result().body());
                promise.complete(responseBody);

           } else {
                responseBody.put("cause", asyncResult.cause().getMessage());
                responseBody.put("status", asyncResult.result().statusMessage());
		promise.fail(responseBody.encodePrettily());
           }
        });

	return promise.future(); //Future.succeededFuture(responseBody);
    }
}
