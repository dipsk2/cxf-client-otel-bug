package org.example.cxfclientotelbug.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sets up the wiremock to mock the response to cxf client calls
 */
@Configuration
public class WiremockConfig {
    public static final String WIREMOCK_BEAN = "wiremockServer";

    @Bean(WIREMOCK_BEAN)
    WireMockServer wiremockServer(@Value("${wiremock.server.port}") int port) {
        var wireMockServer = new WireMockServer(port);
        wireMockServer.start();
        return wireMockServer;
    }
}
