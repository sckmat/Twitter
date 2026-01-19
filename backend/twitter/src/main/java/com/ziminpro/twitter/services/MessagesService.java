package com.ziminpro.twitter.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ziminpro.twitter.dao.MessageRepository;
import com.ziminpro.twitter.dtos.Constants;
import com.ziminpro.twitter.dtos.Message;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class MessagesService {

    private final MessageRepository messageRepository;

    public MessagesService(MessageRepository messageRepository, UMSConnector umsConnector,
                           @org.springframework.beans.factory.annotation.Value("${ums.paths.user}") String uriUser) {
        this.messageRepository = messageRepository;
    }

    private static Mono<UUID> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth != null && auth.isAuthenticated())
                .map(auth -> UUID.fromString(auth.getName()));
    }

    private static Mono<Boolean> isAdmin() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> auth != null && auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())))
                .defaultIfEmpty(false);
    }

    public Mono<ResponseEntity<Map<String, Object>>> createMessage(Message message, String authorization) {
        if (message == null) {
            return Mono.just(okJson(resp("400", "Message body is required", false)));
        }
        if (message.getAuthor() == null) {
            return Mono.just(okJson(resp("400", "Message author is required", false)));
        }

        return Mono.zip(currentUserId(), isAdmin())
                .flatMap(t -> {
                    UUID currentUserId = t.getT1();
                    boolean admin = t.getT2();

                    UUID authorId = message.getAuthor();

                    if (!admin && !currentUserId.equals(authorId)) {
                        return Mono.just(okJson(resp("403", "You can create messages only as yourself", false)));
                    }

                    UUID messageId = messageRepository.createMessage(message);
                    if (messageId == null) {
                        return Mono.just(okJson(resp("500", "Message has not been created", false)));
                    }

                    return Mono.just(okJson(resp("201", "Message has been created", messageId.toString())));
                });
    }

    public Mono<ResponseEntity<Map<String, Object>>> getMessagebyId(UUID messageId, String authorization) {
        Message message = messageRepository.getMessagebyId(messageId);

        if (message == null || message.getId() == null) {
            return Mono.just(okJson(resp("404", "Message not found", new HashMap<>())));
        }

        return Mono.just(okJson(resp("200", "Message has been found", message)));
    }

    public Mono<ResponseEntity<Map<String, Object>>> getMessagesForProducerById(UUID producerId, String authorization) {
        List<Message> messages = messageRepository.getMessagesForProducerById(producerId);

        if (messages == null || messages.isEmpty()) {
            return Mono.just(okJson(resp("404", "Either producer didn't produce any messages or producer not found", new ArrayList<>())));
        }

        return Mono.just(okJson(resp("200", "List of messages has been requested successfully", messages)));
    }

    public Mono<ResponseEntity<Map<String, Object>>> getMessagesForSubscriberById(UUID subscriberId, String authorization) {
        List<Message> messages = messageRepository.getMessagesForSubscriberById(subscriberId);
        if (messages == null || messages.isEmpty()) {
            return Mono.just(okJson(resp("404", "Subscription not found or empty", new ArrayList<>())));
        }
        return Mono.just(okJson(resp("200", "List of messages has been requested successfully", messages)));
    }

    public Mono<ResponseEntity<Map<String, Object>>> deleteMessageById(UUID messageId, String authorization) {
        return Mono.zip(currentUserId(), isAdmin())
                .flatMap(t -> {
                    UUID currentUserId = t.getT1();
                    boolean admin = t.getT2();

                    Message message = messageRepository.getMessagebyId(messageId);
                    if (message == null || message.getId() == null) {
                        return Mono.just(okJson(resp("404", "Message not found", false)));
                    }

                    UUID ownerId = message.getAuthor();
                    if (ownerId == null) {
                        return Mono.just(okJson(resp("500", "Message owner is not set", false)));
                    }

                    if (!admin && !currentUserId.equals(ownerId)) {
                        return Mono.just(okJson(resp("403", "You can delete only your own messages", false)));
                    }

                    int result = messageRepository.deleteMessageById(messageId);
                    if (result != 1) {
                        return Mono.just(okJson(resp("500", "Message " + messageId + " has not been deleted", false)));
                    }

                    return Mono.just(okJson(resp("200", "Message " + messageId + " successfully deleted", true)));
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