package com.example.kafkaconsumer.consumer;

// Интерфейс для обработки сущностей
public interface EntityProcessor<T> {
    void process(T entity);  // Метод для обработки переданной сущности
}