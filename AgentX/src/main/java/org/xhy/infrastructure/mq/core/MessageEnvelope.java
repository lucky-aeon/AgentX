package org.xhy.infrastructure.mq.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.xhy.infrastructure.mq.enums.EventType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Canonical message envelope wrapping any payload with metadata.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class MessageEnvelope<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String traceId;
    private String description;
    private T data;
    private Long timestamp;
    private List<EventType> eventTypes;

    public MessageEnvelope() {
        // for json
    }

    private MessageEnvelope(String traceId, String description, T data, Long timestamp, List<EventType> eventTypes) {
        this.traceId = traceId;
        this.description = description;
        this.data = data;
        this.timestamp = timestamp;
        this.eventTypes = eventTypes;
    }

    public static <T> Builder<T> builder(T data) {
        return new Builder<>(data);
    }

    public String getTraceId() {
        return traceId;
    }

    public String getDescription() {
        return description;
    }

    public T getData() {
        return data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public List<EventType> getEventTypes() {
        return eventTypes;
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize message envelope", e);
        }
    }

    public static final class Builder<T> {

        private final T data;
        private String description;
        private final List<EventType> eventTypes = new ArrayList<>();
        private String traceId;
        private Long timestamp;

        private Builder(T data) {
            this.data = Objects.requireNonNull(data, "data");
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> addEventType(EventType type) {
            if (type != null) {
                this.eventTypes.add(type);
            }
            return this;
        }

        public Builder<T> eventTypes(List<EventType> types) {
            if (types != null) {
                this.eventTypes.clear();
                this.eventTypes.addAll(types);
            }
            return this;
        }

        public Builder<T> traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder<T> timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public MessageEnvelope<T> build() {
            String tid = traceId;
            if (tid == null) {
                tid = MDC.get(MessageHeaders.TRACE_ID);
            }
            if (tid == null) {
                tid = UUID.randomUUID().toString();
            }
            Long ts = timestamp != null ? timestamp : Instant.now().toEpochMilli();
            return new MessageEnvelope<>(tid, description, data, ts, new ArrayList<>(eventTypes));
        }
    }
}

