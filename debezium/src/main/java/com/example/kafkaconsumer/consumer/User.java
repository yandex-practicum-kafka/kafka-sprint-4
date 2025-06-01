package com.example.kafkaconsumer.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// Класс, представляющий сущность пользователя
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private int id;  // Идентификатор пользователя
    private String name;  // Имя пользователя
    private String email;  // Электронная почта пользователя

    @JsonProperty("created_at")  // Указывает, как поле будет представлено в JSON
    private Long createdAt;  // Дата создания пользователя в формате миллисекунд

    // Метод для получения отформатированной даты создания пользователя
    public String getFormattedCreatedAt() {
        if (createdAt == null) return "null";

        // Предполагаем, что это миллисекунды
//        return LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneOffset.UTC)
//                             .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Если наносекунды - используем:
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(createdAt / 1_000_000_000L, (createdAt % 1_000_000_000L) * 1000000), ZoneOffset.UTC)
                             .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
