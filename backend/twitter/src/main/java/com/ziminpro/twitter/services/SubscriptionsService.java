package com.ziminpro.twitter.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ziminpro.twitter.dao.SubscriptionRepository;
import com.ziminpro.twitter.dtos.Constants;
import com.ziminpro.twitter.dtos.HttpResponseExtractor;
import com.ziminpro.twitter.dtos.Roles;
import com.ziminpro.twitter.dtos.Subscription;
import com.ziminpro.twitter.dtos.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class SubscriptionsService {

    private final SubscriptionRepository subscriptionRepository;
    private final UMSConnector umsConnector;
    private final String uriUser;

    public SubscriptionsService(
            SubscriptionRepository subscriptionRepository,
            UMSConnector umsConnector,
            @Value("${ums.paths.user}") String uriUser
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.umsConnector = umsConnector;
        this.uriUser = uriUser;
    }

    public Mono<ResponseEntity<Map<String, Object>>> getSubscriptionsForSubscriberById(UUID subscriberId, String authorization) {
        return umsConnector
                .retrieveUmsData(uriUser + "/" + subscriberId, authorization)
                .map(res -> {
                    User user = HttpResponseExtractor.extractDataFromHttpClientResponse(res, User.class);

                    if (!user.hasRole(Roles.SUBSCRIBER)) {
                        return okJson(resp("403", "Only SUBSCRIBER can view subscriptions", new Subscription()));
                    }

                    Subscription subscriptions = subscriptionRepository.getSubscription(subscriberId);
                    if (subscriptions == null || subscriptions.getSubscriber() == null) {
                        return okJson(resp("404", "Subscriptions for user with ID " + subscriberId + " is not found", new Subscription()));
                    }

                    return okJson(resp("200", "Subscriptions have been retrieved", subscriptions));
                });
    }

    public Mono<ResponseEntity<Map<String, Object>>> createSubscription(Subscription subscription, String authorization) {
        return umsConnector
                .retrieveUmsData(uriUser + "/" + subscription.getSubscriber(), authorization)
                .map(res -> {
                    User user = HttpResponseExtractor.extractDataFromHttpClientResponse(res, User.class);

                    if (!user.hasRole(Roles.SUBSCRIBER)) {
                        return okJson(resp("403", "Only SUBSCRIBER can create subscription", false));
                    }

                    boolean created = subscriptionRepository.createSubscription(subscription);
                    if (!created) {
                        return okJson(resp("500", "Subscriptions has not been created", false));
                    }

                    return okJson(resp("200", "Subscription has been created", true));
                });
    }

    public Mono<ResponseEntity<Map<String, Object>>> updateSubscriptionForSubscriberById(Subscription subscription, String authorization) {
        return umsConnector
                .retrieveUmsData(uriUser + "/" + subscription.getSubscriber(), authorization)
                .map(res -> {
                    User user = HttpResponseExtractor.extractDataFromHttpClientResponse(res, User.class);

                    if (!user.hasRole(Roles.SUBSCRIBER)) {
                        return okJson(resp("403", "Only SUBSCRIBER can update subscription", false));
                    }

                    boolean updated = subscriptionRepository.updateSubscription(subscription);
                    if (!updated) {
                        return okJson(resp("500", "Subscription has not been updated", false));
                    }

                    return okJson(resp("201", "Subscription has been updated", true));
                });
    }

    public Mono<ResponseEntity<Map<String, Object>>> deleteSubscriptionForSubscriberById(UUID subscriberId, String authorization) {
        return umsConnector
                .retrieveUmsData(uriUser + "/" + subscriberId, authorization)
                .map(res -> {
                    User user = HttpResponseExtractor.extractDataFromHttpClientResponse(res, User.class);

                    if (!user.hasRole(Roles.SUBSCRIBER)) {
                        return okJson(resp("403", "Only SUBSCRIBER can delete subscription", false));
                    }

                    boolean deleted = subscriptionRepository.deleteSubscription(subscriberId);
                    if (!deleted) {
                        return okJson(resp("500", "Subscription has not been deleted", false));
                    }

                    return okJson(resp("201", "Subscription has been deleted", true));
                });
    }

    private static Map<String, Object> resp(String code, String message, Object data) {
        Map<String, Object> r = new HashMap<>();
        r.put(Constants.CODE, code);
        r.put(Constants.MESSAGE, message);
        r.put(Constants.DATA, data);
        return r;
    }

    private static ResponseEntity<Map<String, Object>> okJson(Map<String, Object> body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .header(Constants.ACCEPT, Constants.APPLICATION_JSON)
                .body(body);
    }
}