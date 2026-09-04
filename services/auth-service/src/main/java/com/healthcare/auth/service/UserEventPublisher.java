package com.healthcare.auth.service;

import com.healthcare.auth.config.AuthProperties;
import com.healthcare.auth.entity.Role;
import com.healthcare.auth.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes user lifecycle events.
 *
 * <p>Phase 1 design:
 * <ul>
 *   <li>Kafka is OPTIONAL. If {@code app.events.publish-enabled=false}
 *       (the default in local) the publisher is a no-op.</li>
 *   <li>If Kafka is enabled but unreachable, authentication still
 *       succeeds — failures are logged at WARN, never thrown to the
 *       caller. The platform is being built so other services can
 *       eventually consume these events; we will not let Kafka be a
 *       single point of failure for login/registration.</li>
 *   <li>Events carry the standard envelope described in
 *       {@code docs/events.md}: {@code eventId}, {@code eventType},
 *       {@code eventVersion}, {@code occurredAt}, {@code producer},
 *       {@code correlationId}, {@code payload}.</li>
 * </ul>
 */
@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);
    private static final String PRODUCER = "auth-service";
    private static final String VERSION  = "1";

    private final AuthProperties props;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;
    private final ObjectMapper objectMapper;

    public UserEventPublisher(AuthProperties props,
                              ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider,
                              ObjectMapper objectMapper) {
        this.props = props;
        this.kafkaTemplateProvider = kafkaTemplateProvider;
        this.objectMapper = objectMapper;
    }

    public void publishUserRegistered(User user, String correlationId) {
        if (!props.getEvents().isPublishEnabled()) {
            log.debug("Event publish disabled; skipping UserRegistered for userId={}", user.getId());
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId",     user.getId().toString());
        payload.put("role",       user.getRole().name());
        // We deliberately do NOT put the raw email in the event body.
        // The downstream services that need it fetch it through the
        // User API over an authenticated channel.
        payload.put("emailHint",  emailHint(user.getEmail()));
        send(props.getEvents().getTopicUserRegistered(),
             "UserRegistered", user.getId(), correlationId, payload);
    }

    public void publishUserDeactivated(UUID userId, Role role, String correlationId) {
        if (!props.getEvents().isPublishEnabled()) {
            log.debug("Event publish disabled; skipping UserDeactivated for userId={}", userId);
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId.toString());
        payload.put("role",   role.name());
        send(props.getEvents().getTopicUserDeactivated(),
             "UserDeactivated", userId, correlationId, payload);
    }

    private void send(String topic, String eventType, UUID partitionKey,
                      String correlationId, Map<String, Object> payload) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId",      UUID.randomUUID().toString());
        envelope.put("eventType",    eventType);
        envelope.put("eventVersion", VERSION);
        envelope.put("occurredAt",   Instant.now().toString());
        envelope.put("producer",     PRODUCER);
        envelope.put("correlationId", correlationId);
        envelope.put("payload",      payload);

        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event {}: {}", eventType, e.getMessage());
            return;
        }
        KafkaTemplate<String, String> template = kafkaTemplateProvider.getIfAvailable();
        if (template == null) {
            log.debug("KafkaTemplate not available; dropping event {}", eventType);
            return;
        }
        try {
            template.send(topic, partitionKey.toString(), json)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish {}: {}", eventType, ex.getMessage());
                        } else {
                            log.debug("Published {} to {} partition {}",
                                    eventType, topic,
                                    res != null ? res.getRecordMetadata().partition() : "?");
                        }
                    });
        } catch (Exception e) {
            // Kafka must NOT be on the critical path of login/registration.
            log.warn("Kafka publish threw synchronously for {}: {}", eventType, e.getMessage());
        }
    }

    /** Returns a non-reversible hint of the email (e.g. "j***@example.com"). */
    private static String emailHint(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return "**" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
