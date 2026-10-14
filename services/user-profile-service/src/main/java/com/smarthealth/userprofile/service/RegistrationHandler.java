package com.smarthealth.userprofile.service;

import com.smarthealth.userprofile.model.ProcessedEvent;
import com.smarthealth.userprofile.model.OutboxEvent;
import com.smarthealth.userprofile.repository.OutboxRepository;
import com.smarthealth.userprofile.model.ProfileEntity;
import com.smarthealth.userprofile.repository.ProfileRepository;
import com.smarthealth.userprofile.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.UUID;

@Service
public class RegistrationHandler {
    private final ProfileRepository profiles;
    private final OutboxRepository outbox;
    private final ProcessedEventRepository processed;
    private final JsonMapper json;

    public RegistrationHandler(ProfileRepository profiles, OutboxRepository outbox,
                               ProcessedEventRepository processed, JsonMapper json) {
        this.profiles = profiles;
        this.outbox = outbox;
        this.processed = processed;
        this.json = json;
    }

    /** Profile + outbox row + processed marker in ONE local transaction. */
    @Transactional
    public void handle(String payload) {
        JsonNode n = parse(payload);
        String eventId = field(n, "eventId");
        String userId = field(n, "userId");
        String email = field(n, "email");

        if (processed.existsById(eventId)) return;              // idempotent consumer

        if (!profiles.existsById(userId)) {
            profiles.save(new ProfileEntity(userId, email));
        }
        outbox.save(OutboxEvent.of("user.profile.created", userId, envelope(userId, null)));
        processed.save(new ProcessedEvent(eventId));
    }

    @Transactional
    public void recordFailure(String key, String reason) {
        String userId = key == null ? "unknown" : key;
        outbox.save(OutboxEvent.of("user.profile.failed", userId, envelope(userId, reason)));
    }

    // ADJUST to contracts/events/user.profile.created.v1.json and user.profile.failed.v1.json:
    // those schemas have additionalProperties=false, so field names must match exactly.
    private String envelope(String userId, String reason) {
        ObjectNode o = json.createObjectNode();
        o.put("eventId", UUID.randomUUID().toString());
        o.put("occurredAt", Instant.now().toString());
        o.put("userId", userId);
        if (reason != null) o.put("reason", reason);
        return o.toString();
    }

    private JsonNode parse(String payload) {
        try {
            return json.readTree(payload);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Unparseable event payload");
        }
    }

    private static String field(JsonNode n, String name) {
        JsonNode v = n.get(name);
        if (v == null || v.isNull() || v.asString().isBlank()) {
            throw new IllegalArgumentException("Missing field: " + name);
        }
        return v.asString();
    }
}