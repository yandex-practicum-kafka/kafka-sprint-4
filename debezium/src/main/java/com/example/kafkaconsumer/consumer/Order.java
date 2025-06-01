package com.example.kafkaconsumer.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// Класс, представляющий сущность заказа
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private int id;  // Идентификатор заказа

    @JsonProperty("user_id")  // Указывает, как поле будет представлено в JSON
    private Integer userId;  // Идентификатор пользователя, сделавшего заказ

    @JsonProperty("product_name")  // Указывает, как поле будет представлено в JSON
    private String productName;  // Название продукта в заказе
    private Integer quantity;  // Количество заказанного продукта

    @JsonProperty("order_date")  // Указывает, как поле будет представлено в JSON
    private Long orderDate;  // Дата заказа в формате миллисекунд

    // Метод для получения отформатированной даты заказа
	public String getFormattedOrderDate() {
        if (orderDate == null) return "null";
        // Преобразуем Long в LocalDateTime и форматируем
//        return LocalDateTime.ofInstant(Instant.ofEpochMilli(orderDate), ZoneOffset.UTC)
//                             .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        // Если нужно использовать наносекунды, замените на:
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(orderDate / 1000000000L, (orderDate % 1000000000L) * 1000000), ZoneOffset.UTC)
                             .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
