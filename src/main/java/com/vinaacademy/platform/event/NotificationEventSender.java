package com.vinaacademy.platform.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import vn.vinaacademy.kafka.KafkaTopic;
import vn.vinaacademy.kafka.event.NotificationCreateEvent;

@Service
@Slf4j
public class NotificationEventSender {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationEventSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void createNotification(NotificationCreateEvent event) {
        Message<NotificationCreateEvent> message = MessageBuilder.withPayload(event)
                .build();

        kafkaTemplate.send(KafkaTopic.NOTIFICATION_TOPIC, message)
                .whenCompleteAsync(
                        (result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send notification event: {}", ex.getMessage(), ex);
                            } else {
                                log.info("Notification event sent successfully: {}", result.getProducerRecord().value());
                            }
                        }
                );
    }
}
