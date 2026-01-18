package com.ziminpro.twitter.services;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class UMSConnector {

    private final WebClient client;

    public UMSConnector(
            WebClient.Builder builder,
            @Value("${ums.host}") String umsHost,
            @Value("${ums.port}") String umsPort
    ) {
        this.client = builder
                .baseUrl(umsHost + ":" + umsPort)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<Object> retrieveUmsData(String uri, String authorizationHeader) {
        return client.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .accept(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .retrieve()
                .bodyToMono(Object.class);
    }
}
