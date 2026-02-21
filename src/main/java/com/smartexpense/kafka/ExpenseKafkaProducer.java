package com.smartexpense.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseKafkaProducer {

    private final KafkaTemplate<String, ExpenseUploadMessage> kafkaTemplate;

    @Value("${kafka.topics.expense-upload}")
    private String topic;

    public void publish(ExpenseUploadMessage message) {
        String key = message.getBatchId() + "-" + message.getRowNumber();
        kafkaTemplate.send(topic, key, message);
        log.debug("📤 Published row {} of batch {} to Kafka", message.getRowNumber(), message.getBatchId());
    }
}
