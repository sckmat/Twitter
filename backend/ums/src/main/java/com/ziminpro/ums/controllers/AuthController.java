package com.ziminpro.ums.controllers;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.List;

import com.ziminpro.ums.auth.GitHubOAuthService;
import com.ziminpro.ums.auth.JwtService;
import com.ziminpro.ums.dao.UmsRepository;
import com.ziminpro.ums.dtos.Constants;
import com.ziminpro.ums.dtos.Roles;
import com.ziminpro.ums.dtos.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@RestController
public class AuthController {

    private static final String STATE_COOKIE = "oauth_state";

    @Autowired
    private UmsRepository umsRepository;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private JwtService jwtService;

    @Value("${oauth.github.frontend-redirect:http://localhost:3000/}")
    private String frontendRedirect;

    private final Map<String, Object> response = new HashMap<>();

    @RequestMapping(method = RequestMethod.POST, path = "/auth/login", consumes = Constants.APPLICATION_JSON)
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody Map<String, String> body) {
        response.clear();

        String email = body == null ? null : body.get("login");
        String password = body == null ? null : body.get("password");

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.put(Constants.CODE, "400");
            response.put(Constants.MESSAGE, "Email and password are required");
            response.put(Constants.DATA, new HashMap<>());
            return okJson(response);
        }

        email = email.trim().toLowerCase();

        User user = umsRepository.findUserByEmail(email);

        if (user == null || user.getId() == null) {
            response.put(Constants.CODE, "401");
            response.put(Constants.MESSAGE, "User not found");
            response.put(Constants.DATA, new HashMap<>());
            return okJson(response);
        }

        if (user.getPassword() == null || !user.getPassword().equals(password)) {
            response.put(Constants.CODE, "401");
            response.put(Constants.MESSAGE, "Invalid credentials");
            response.put(Constants.DATA, new HashMap<>());
            return okJson(response);
        }

        List<String> roleNames = user.getRoles() == null ? List.of() :
                user.getRoles().stream()
                        .map(Roles::getRole)
                        .filter(r -> r != null && !r.isBlank())
                        .distinct()
                        .collect(Collectors.toList());

        String token = jwtService.issueToken(user.getId(), user.getEmail(), roleNames);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);

        response.put(Constants.CODE, "200");
        response.put(Constants.MESSAGE, "Login success");
        response.put(Constants.DATA, data);

        return okJson(response);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/auth/github")
    public Mono<ResponseEntity<Void>> githubStart() {
        if (!gitHubOAuthService.isConfigured()) {
            return Mono.just(ResponseEntity.status(500).build());
        }

        String state = gitHubOAuthService.generateState();
        String authorizeUrl = gitHubOAuthService.buildAuthorizeUrl(state);

        ResponseCookie cookie = ResponseCookie.from(STATE_COOKIE, state)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(300)
                .build();

        return Mono.just(ResponseEntity.status(302)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .location(URI.create(authorizeUrl))
                .build());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/auth/github/callback")
    public Mono<ResponseEntity<Void>> githubCallback(
            ServerWebExchange exchange,
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state
    ) {
        String cookieState = readCookie(exchange, STATE_COOKIE);
        if (cookieState == null || state == null || !cookieState.equals(state)) {
            return Mono.just(redirect(frontendRedirect + "?error=oauth_state"));
        }

        return gitHubOAuthService.exchangeCodeForAccessToken(code)
                .flatMap(gitHubOAuthService::fetchUserProfile)
                .map(profile -> {
                    String email = profile.email();
                    if (email == null || email.isBlank()) {
                        return redirect(frontendRedirect + "?error=no_email");
                    }

                    email = email.trim().toLowerCase();

                    User user = umsRepository.findUserByEmail(email);
                    if (user == null || user.getId() == null) {
                        user = createUserFromGithub(profile.name(), email);
                    }

                    List<String> roleNames = user.getRoles() == null ? List.of() :
                            user.getRoles().stream()
                                    .map(Roles::getRole)
                                    .filter(r -> r != null && !r.isBlank())
                                    .distinct()
                                    .collect(Collectors.toList());

                    String token = jwtService.issueToken(user.getId(), email, roleNames);
                    String safeToken = java.net.URLEncoder.encode(token, StandardCharsets.UTF_8);

                    return redirect(frontendRedirect + "?token=" + safeToken);
                });
    }

    private User createUserFromGithub(String name, String email) {
        Map<String, Roles> roles = umsRepository.findAllRoles();

        Roles defaultRole = roles.getOrDefault(
                "PRODUCER",
                roles.getOrDefault(
                        "ROLE_PRODUCER",
                        roles.values().stream()
                                .findFirst()
                                .orElse(new Roles(null, "PRODUCER", "Default"))
                )
        );

        User user = new User();
        user.setName((name == null || name.isBlank()) ? extractNameFromEmail(email) : name);
        user.setEmail(email);
        user.setPassword(UUID.randomUUID().toString());
        user.addRole(new Roles(null, defaultRole.getRole(), null));

        UUID id = umsRepository.createUser(user);
        user.setId(id);

        return user;
    }

    private static String extractNameFromEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) return "user";
        String local = email.substring(0, at).trim();
        return local.isEmpty() ? "user" : local;
    }

    private static String readCookie(ServerWebExchange exchange, String name) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(name);
        return cookie == null ? null : cookie.getValue();
    }

    private static ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    private static Mono<ResponseEntity<Map<String, Object>>> okJson(Map<String, Object> body) {
        return Mono.just(ResponseEntity.ok()
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .header(Constants.ACCEPT, Constants.APPLICATION_JSON)
                .body(body));
    }
}