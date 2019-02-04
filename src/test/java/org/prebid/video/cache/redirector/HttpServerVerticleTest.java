package org.prebid.video.cache.redirector;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class HttpServerVerticleTest {

    private static final int PORT = 8080;
    private static final Map<String, String> REDIRECT_MAPPING = Collections.singletonMap(
            "prebid-server.rubiconproject.com", "https://prebid-server.rubiconproject.com/cache?uuid={{uuid}}");

    private static WebClient webClient;

    @BeforeAll
    @DisplayName("Deploy a verticle and initialize web client")
    public static void setUp(Vertx vertx, VertxTestContext testContext) {

        final DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("http.port", PORT)
                        .put("redirect.mapping", REDIRECT_MAPPING));

        vertx.deployVerticle(new HttpServerVerticle(), options, testContext.completing());
        webClient = WebClient.create(vertx, new WebClientOptions().setFollowRedirects(false));
    }

    @Test
    @DisplayName("Positive: known host should result in 302 with location header")
    public void shouldRespondWith302AndLocation(VertxTestContext testContext) {
        webClient.get(8080, "localhost", "/redir")
                .addQueryParam("host", "prebid-server.rubiconproject.com")
                .addQueryParam("uuid", "123")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(302, response.statusCode());
                    assertEquals("https://prebid-server.rubiconproject.com/cache?uuid=123",
                            response.getHeader("Location"));
                    testContext.completeNow();
                })));
    }

    @Test
    @DisplayName("Negative: unknown host should result in 404")
    public void shouldRespondWith404IfHostUnknown(VertxTestContext testContext) {
        webClient.get(8080, "localhost", "/redir")
                .addQueryParam("host", "prebid.example.com")
                .addQueryParam("uuid", "123")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(404, response.statusCode());
                    assertEquals("Unknown host", response.bodyAsString());
                    testContext.completeNow();
                })));
    }

    @Test
    @DisplayName("Negative: missing uuid should result in 400")
    public void shouldRespondWith400IfUuidMissing(VertxTestContext testContext) {
        webClient.get(8080, "localhost", "/redir")
                .addQueryParam("host", "prebid-server.rubiconproject.com")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("Request should contain single host and uuid query parameters",
                            response.bodyAsString());
                    testContext.completeNow();
                })));
    }

    @Test
    @DisplayName("Negative: missing host should result in 400")
    public void shouldRespondWith400IfHostMissing(VertxTestContext testContext) {
        webClient.get(8080, "localhost", "/redir")
                .addQueryParam("uuid", "123")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("Request should contain single host and uuid query parameters",
                            response.bodyAsString());
                    testContext.completeNow();
                })));
    }

    @Test
    @DisplayName("CORS: OPTIONS response should allow passed origin")
    public void shouldRespondWithAllowedOrigin(VertxTestContext testContext) {
        webClient.request(HttpMethod.OPTIONS, 8080, "localhost", "")
                .putHeader("Origin", "http://example.com")
                .putHeader("Access-Control-Request-Method", "GET")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(200, response.statusCode());
                    assertEquals("http://example.com", response.getHeader("Access-Control-Allow-Origin"));
                    testContext.completeNow();
                })));
    }

    @Test
    @DisplayName("Positive: health endpoint returns 200")
    public void healthShouldRespondWith200(VertxTestContext testContext) {
        webClient.get(8080, "localhost", "/health")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(200, response.statusCode());
                    testContext.completeNow();
                })));
    }
}
