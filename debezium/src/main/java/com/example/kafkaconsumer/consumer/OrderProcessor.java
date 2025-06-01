package com.example.kafkaconsumer.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Класс для обработки данных о заказах
@Slf4j
@Component
public class OrderProcessor implements EntityProcessor<Order> {

	@Override
	public void process(Order order) {
		
		// Обработка данных заказа
		log.info("Order processing...");
		
		// Журналирование данных заказа
		log.info("Order: id={}, userId={}, productName={}, quantity={}, orderDate={}",
			order.getId(),
			order.getUserId(),
			order.getProductName(),
			order.getQuantity(),
			order.getFormattedOrderDate()); 
	}

}
