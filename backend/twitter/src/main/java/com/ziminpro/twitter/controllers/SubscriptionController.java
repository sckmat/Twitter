package com.ziminpro.twitter.controllers;

import java.util.Map;
import java.util.UUID;

import com.ziminpro.twitter.dtos.Constants;
import com.ziminpro.twitter.dtos.Subscription;
import com.ziminpro.twitter.services.SubscriptionsService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(Constants.URI_SUBSCRIPTION)
public class SubscriptionController {

    private final SubscriptionsService subscriptions;

    public SubscriptionController(SubscriptionsService subscriptions) {
        this.subscriptions = subscriptions;
    }

    @GetMapping("/{subscriber-id}")
    public Mono<ResponseEntity<Map<String, Object>>> getSubscriptionsForSubscriberById(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable("subscriber-id") UUID subscriberId
    ) {
        return subscriptions.getSubscriptionsForSubscriberById(subscriberId, authorization);
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> createSubscription(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody Subscription subscription
    ) {
        return subscriptions.createSubscription(subscription, authorization);
    }

    @PutMapping
    public Mono<ResponseEntity<Map<String, Object>>> updateSubscriptionForSubscriberById(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody Subscription subscription
    ) {
        return subscriptions.updateSubscriptionForSubscriberById(subscription, authorization);
    }

    @DeleteMapping("/{subscriber-id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteSubscriptionForSubscriberById(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable("subscriber-id") UUID subscriberId
    ) {
        return subscriptions.deleteSubscriptionForSubscriberById(subscriberId, authorization);
    }
}