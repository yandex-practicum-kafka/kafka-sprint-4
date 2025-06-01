package com.example.kafkaconsumer.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@EnableKafka  // Включает поддержку Kafka в приложении
@Configuration  // Указывает, что класс является источником конфигурации для Spring
public class KafkaConsumerConfig {

    // Получаем адреса Kafka-брокеров из конфигурационного файла
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Получаем идентификатор группы потребителей из конфигурационного файла
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // Получаем настройку auto.offset.reset из конфигурационного файла
    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    // Получаем десериализатор ключей из конфигурационного файла
    @Value("${spring.kafka.consumer.key-deserializer}")
    private String keyDeserializer;

    // Получаем десериализатор значений из конфигурационного файла
    @Value("${spring.kafka.consumer.value-deserializer}")
    private String valueDeserializer;

    // Создаем фабрику потребителей для настроек Kafka
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);  // Устанавливаем адреса брокеров
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);  // Устанавливаем идентификатор группы потребителей
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);  // Устанавливаем десериализатор ключей
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);  // Устанавливаем десериализатор значений
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);  // Устанавливаем поведение при отсутствии смещения
        return new DefaultKafkaConsumerFactory<>(props);  // Возвращаем настроенную фабрику потребителей
    }

    // Создаем контейнер для прослушивания сообщений Kafka
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();  // Инициализируем контейнер
        factory.setConsumerFactory(consumerFactory());  // Подключаем фабрику потребителей
        return factory;  // Возвращаем настроенный контейнер
    }
}
