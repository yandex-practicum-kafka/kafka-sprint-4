package com.example.kafkaconsumer.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GenericKafkaListener {

    // ObjectMapper для парсинга JSON сообщений из Kafka
    private final ObjectMapper objectMapper;

    // Обработчики для сущностей User и Order
    private final EntityProcessor<User> userProcessor;
    private final EntityProcessor<Order> orderProcessor;

    // Конструктор с внедрением зависимостей
    public GenericKafkaListener(UserProcessor userProcessor, OrderProcessor orderProcessor) {
        this.userProcessor = userProcessor;
        this.orderProcessor = orderProcessor;

        this.objectMapper = new ObjectMapper();
        // Регистрация модуля для обработки Java 8 дат и времени, если используете LocalDateTime и т.п.
        this.objectMapper.registerModule(new JavaTimeModule());
        // Отключение использования временных штампов при сериализации дат
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // Метод обработки сообщений из топика пользователей
    @KafkaListener(topics = "${kafka.topics.users}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUser(String message) {
        handleMessage(message, User.class, userProcessor);
    }

    // Метод обработки сообщений из топика заказов
    @KafkaListener(topics = "${kafka.topics.orders}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrder(String message) {
        handleMessage(message, Order.class, orderProcessor);
    }

    /**
     * Обобщенный метод обработки сообщений от Kafka.
     * @param message - JSON сообщение
     * @param clazz - класс для десериализации (User или Order)
     * @param processor - обработчик для конкретной сущности
     */
    private <T> void handleMessage(String message, Class<T> clazz, EntityProcessor<T> processor) {
        try {
            // Разбор JSON сообщения
            JsonNode root = objectMapper.readTree(message);
            // Получение блока "payload"
            JsonNode payload = root.get("payload");
            if (payload == null) {
                log.warn("Payload missing, message invalid");
                return;
            }

            // Получение данных после изменения (новых данных)
            JsonNode afterNode = payload.get("after");
            if (afterNode != null && !afterNode.isNull()) {
                // Преобразование JSON в объект нужного класса
                T entity = objectMapper.treeToValue(afterNode, clazz);
                // Обработка полученного объекта
                processor.process(entity);
            } else {
                // В случае, если "after" отсутствует или null - это может быть удаление или обновление
                JsonNode beforeNode = payload.get("before");
                log.info("Received delete or update event for {}: {}", clazz.getSimpleName(), beforeNode);
                // Здесь можно добавить логику обработки удаления или обновления, если необходимо
            }
        } catch (Exception e) {
            // Логируем ошибки парсинга или обработки
            log.error("Error processing message", e);
        }
    }
}
