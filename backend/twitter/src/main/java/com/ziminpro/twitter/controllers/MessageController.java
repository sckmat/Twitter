package com.ziminpro.twitter.controllers;

import java.util.Map;
import java.util.UUID;

import com.ziminpro.twitter.dtos.Constants;
import com.ziminpro.twitter.dtos.Message;
import com.ziminpro.twitter.services.MessagesService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(Constants.URI_MESSAGE)
public class MessageController {

    private final MessagesService messages;

    public MessageController(MessagesService messages) {
        this.messages = messages;
    }

    @GetMapping("/{message-id}")
    public Mono<ResponseEntity<Map<String, Object>>> getMessageById(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable("message-id") UUID messageId
    ) {
        return messages.getMessagebyId(messageId, authorization);
    }

    @GetMapping("/producer/{producer-id}")
    public Mono<ResponseEntity<Map<String, Object>>> getMessagesForProducerById(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable("producer-id") UUID producerId
    ) {
        return messages.getMessagesForProducerById(producerId, authorization);
    }

    @GetMapping("/subscriber/{subscriber-id}")
    public Mono<ResponseEntity<Map<String, Object>>> getMessagesForSubscriberById(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable("subscriber-id") UUID subscriberId
    ) {
        return messages.getMessagesForSubscriberById(subscriberId, authorization);
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> createMessage(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody Message message
    ) {
        return messages.createMessage(message, authorization);
    }

    @DeleteMapping("/{message-id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteMessageById(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable("message-id") UUID messageId
    ) {
        return messages.deleteMessageById(messageId, authorization);
    }
}