package edu.ucla.library.libcal.services;

import edu.ucla.library.libcal.JsonKeys;
import edu.ucla.library.libcal.utils.TokenUtils;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigStoreOptions;
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
    public Future<JsonObject> getConfig() {
        final Promise<JsonObject> promise = Promise.promise();
        final ConfigStoreOptions envPropsStore = new ConfigStoreOptions().setType("env");
        final ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys");
        final ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(envPropsStore).addStore(sysPropsStore);
        final ConfigRetriever retriever = ConfigRetriever.create(myVertx, options);

        retriever.getConfig(configResult -> {
           if (configResult.succeeded()) {
             promise.complete(configResult.result());
           } else {
             promise.fail(configResult.cause().getMessage());
           }      
        });

	return promise.future();
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

	return promise.future();
    }

    @Override
    public Future<Void> close() {
        return Future.succeededFuture();
    }
}
