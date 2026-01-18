package com.ziminpro.twitter.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ziminpro.twitter.dao.MessageRepository;
import com.ziminpro.twitter.dtos.Constants;
import com.ziminpro.twitter.dtos.HttpResponseExtractor;
import com.ziminpro.twitter.dtos.Message;
import com.ziminpro.twitter.dtos.Roles;
import com.ziminpro.twitter.dtos.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class MessagesService {

    private final MessageRepository messageRepository;
    private final UMSConnector umsConnector;
    private final String uriUser;

    public MessagesService(
            MessageRepository messageRepository,
            UMSConnector umsConnector,
            @Value("${ums.paths.user}") String uriUser
    ) {
        this.messageRepository = messageRepository;
        this.umsConnector = umsConnector;
        this.uriUser = uriUser;
    }

    public Mono<ResponseEntity<Map<String, Object>>> createMessage(Message message, String authorization) {
        return umsConnector
                .retrieveUmsData(uriUser + "/" + message.getAuthor(), authorization)
                .map(res -> {
                    User user = HttpResponseExtractor.extractDataFromHttpClientResponse(res, User.class);

                    if (!user.hasRole(Roles.PRODUCER)) {
                        return okJson(resp("403", "Only PRODUCER can create messages", false));
                    }

                    UUID messageId = messageRepository.createMessage(message);
                    if (messageId == null) {
                        return okJson(resp("500", "Message has not been created", false));
                    }

                    return okJson(resp("201", "Message has been created", messageId.toString()));
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
        return umsConnector
                .retrieveUmsData(uriUser + "/" + subscriberId, authorization)
                .map(res -> {
                    User user = HttpResponseExtractor.extractDataFromHttpClientResponse(res, User.class);

                    if (!user.hasRole(Roles.SUBSCRIBER)) {
                        return okJson(resp("403", "Only SUBSCRIBER can request subscriber feed", new ArrayList<>()));
                    }

                    List<Message> messages = messageRepository.getMessagesForSubscriberById(subscriberId);
                    if (messages == null || messages.isEmpty()) {
                        return okJson(resp("404", "Subscription not found or empty", new ArrayList<>()));
                    }

                    return okJson(resp("200", "List of messages has been requested successfully", messages));
                });
    }

    public Mono<ResponseEntity<Map<String, Object>>> deleteMessageById(UUID messageId, String authorization) {
        int result = messageRepository.deleteMessageById(messageId);

        if (result != 1) {
            return Mono.just(okJson(resp("500", "Message " + messageId + " has not been deleted", false)));
        }

        return Mono.just(okJson(resp("200", "Message " + messageId + " successfully deleted", true)));
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