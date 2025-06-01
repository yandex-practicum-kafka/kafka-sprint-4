package com.example.kafkaconsumer.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Класс для обработки данных пользователей
@Slf4j
@Component
public class UserProcessor implements EntityProcessor<User> {

	@Override
	public void process(User user) {

		// Обработка данных пользователя
		log.info("User processing...");

		// Журналирование данных пользователя
		log.info("User: id={}, name={}, email={}, createdAt={}",
			user.getId(),
			user.getName(),
			user.getEmail(),
			user.getFormattedCreatedAt());
	}

}
