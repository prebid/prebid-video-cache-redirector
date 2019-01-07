package org.prebid.video.cache.redirector;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpServerVerticle extends AbstractVerticle {

    private static final String REDIR_ENDPOINT = "/redir";
    private static final String HOST_QUERY_PARAM = "host";
    private static final String UUID_QUERY_PARAM = "uuid";

    private static final String HEALTH_ENDPOINT = "/health";

    private static final String UUID_PLACEHOLDER = "{{uuid}}";

    private Map<String, String> mapping;

    @Override
    public void start(Future<Void> future) {
        final int port = config().getInteger("http.port");
        mapping = config().getJsonObject("redirect.mapping")
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));

        final Router router = Router.router(vertx);

        router.get(REDIR_ENDPOINT).handler(this::handleRedir);
        router.get(HEALTH_ENDPOINT).handler(this::handleHealth);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port, result -> {
                    if (result.succeeded()) {
                        future.complete();
                    } else {
                        future.fail(result.cause());
                    }
                });
    }

    private void handleRedir(RoutingContext context) {
        final List<String> hostValues = context.queryParam(HOST_QUERY_PARAM);
        final List<String> uuidValues = context.queryParam(UUID_QUERY_PARAM);

        if (hostValues.isEmpty() || hostValues.size() > 1 || uuidValues.isEmpty() || uuidValues.size() > 1) {
            context.response()
                    .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .end("Request should contain single host and uuid query parameters");
            return;
        }

        final String host = hostValues.get(0);

        if (!mapping.containsKey(host)) {
            context.response()
                    .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                    .end("Unknown host");
            return;
        }

        final String location = mapping.get(host).replace(UUID_PLACEHOLDER, uuidValues.get(0));

        context.response()
                .setStatusCode(302)
                .putHeader(HttpHeaders.LOCATION, location)
                .end();
    }

    private void handleHealth(RoutingContext context) {
        context.response().setStatusCode(HttpResponseStatus.OK.code()).end();
    }
}