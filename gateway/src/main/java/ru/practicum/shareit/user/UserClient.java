package ru.practicum.shareit.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dto.UserDto;

@Service
@Slf4j
public class UserClient extends BaseClient {
    private static final String API_PREFIX = "/users";

    @Autowired
    public UserClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .build()
        );
    }

    public ResponseEntity<Object> getUsers() {
        return get("");
    }

    public ResponseEntity<Object> getUserById(long userId) {
        return get("/" +  userId);
    }

    public ResponseEntity<Object> createUser(UserDto user) {
        exceptionUser(user);
        return post("", user);
    }

    public ResponseEntity<Object> updateUser(UserDto user, long userId) {
        return patch("/" + userId, user);
    }

    public  ResponseEntity<Object> deleteUserById(long userId) {
        return delete("/" + userId);
    }

    private void exceptionUser(UserDto user) {
        if (user.getName() == null) {
            log.warn("Валидация не пройдена: логин не был указан");
            throw new ValidationException("Логин не был указан");
        }
        if (user.getName().isBlank()) {
            log.warn("Валидация не пройдена: логин пустой");
            throw new ValidationException("Логин не может быть пустым");
        }
        if (user.getEmail() == null) {
            log.warn("Валидация не пройдена: email не был указан");
            throw new ValidationException("Email не был указан");
        }
        if (user.getEmail().isBlank()) {
            log.warn("Валидация не пройдена: email пустой");
            throw new ValidationException("Указан пустой email");
        }
    }
}
