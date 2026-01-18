package com.ziminpro.ums.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class GitHubOAuthService {

    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";
    private static final String EMAILS_URL = "https://api.github.com/user/emails";

    private final WebClient webClient;
    private final SecureRandom random = new SecureRandom();

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;

    public GitHubOAuthService(
            @Value("${oauth.github.client-id:}") String clientId,
            @Value("${oauth.github.client-secret:}") String clientSecret,
            @Value("${oauth.github.redirect-uri:}") String redirectUri,
            @Value("${oauth.github.scope:read:user user:email}") String scope
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;

        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, "ums")
                .build();
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && redirectUri != null && !redirectUri.isBlank();
    }

    public String generateState() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String buildAuthorizeUrl(String state) {
        return AUTHORIZE_URL
                + "?client_id=" + urlEncode(clientId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&scope=" + urlEncode(scope)
                + "&state=" + urlEncode(state);
    }

    public Mono<String> exchangeCodeForAccessToken(String code) {
        return webClient.post()
                .uri(ACCESS_TOKEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "client_id", clientId,
                        "client_secret", clientSecret,
                        "code", code,
                        "redirect_uri", redirectUri
                ))
                .retrieve()
                .onStatus(s -> s.isError(), r -> r.bodyToMono(String.class)
                        .defaultIfEmpty("GitHub token exchange failed")
                        .flatMap(msg -> Mono.error(new IllegalStateException(msg))))
                .bodyToMono(GitHubTokenResponse.class)
                .map(GitHubTokenResponse::access_token);
    }

    public Mono<GitHubUserProfile> fetchUserProfile(String accessToken) {
        return webClient.get()
                .uri(USER_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(s -> s.isError(), r -> r.bodyToMono(String.class)
                        .defaultIfEmpty("GitHub profile fetch failed")
                        .flatMap(msg -> Mono.error(new IllegalStateException(msg))))
                .bodyToMono(GitHubUserProfile.class)
                .flatMap(profile -> {
                    if (profile.email() != null && !profile.email().isBlank()) {
                        return Mono.just(profile);
                    }
                    return fetchPrimaryEmail(accessToken)
                            .map(email -> new GitHubUserProfile(
                                    profile.id(),
                                    profile.login(),
                                    profile.name(),
                                    email,
                                    profile.avatar_url()
                            ));
                });
    }

    private Mono<String> fetchPrimaryEmail(String accessToken) {
        return webClient.get()
                .uri(EMAILS_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(s -> s.isError(), r -> r.bodyToMono(String.class)
                        .defaultIfEmpty("GitHub emails fetch failed")
                        .flatMap(msg -> Mono.error(new IllegalStateException(msg))))
                .bodyToMono(GitHubEmail[].class)
                .map(List::of)
                .map(emails -> {
                    Optional<GitHubEmail> primaryVerified = emails.stream()
                            .filter(e -> Boolean.TRUE.equals(e.primary()) && Boolean.TRUE.equals(e.verified()))
                            .findFirst();
                    if (primaryVerified.isPresent()) return primaryVerified.get().email();
                    return emails.stream().findFirst().map(GitHubEmail::email).orElse(null);
                });
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public record GitHubTokenResponse(String access_token, String scope, String token_type) {}
    public record GitHubUserProfile(Long id, String login, String name, String email, String avatar_url) {}
    public record GitHubEmail(String email, Boolean primary, Boolean verified, String visibility) {}
}
