package ru.practicum.shareit.request;

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
import ru.practicum.shareit.request.dto.CreateRequestDto;

@Service
@Slf4j
public class RequestClient extends BaseClient {
    private static final String API_PREFIX = "/requests";

    @Autowired
    public RequestClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    public ResponseEntity<Object> createRequest(CreateRequestDto requestDto, long userId) {
        exceptionRequest(requestDto);

        return post("", userId, requestDto);
    }

    public ResponseEntity<Object> getRequests(long userId, String path) {
        return get(path, userId);
    }

    private void exceptionRequest(CreateRequestDto request) {
        if (request.getDescription() == null) {
            log.warn("Валидация не пройдена: описание не было указано");
            throw new ValidationException("Описание не было указано");
        }
        if (request.getDescription().isBlank()) {
            log.warn("Валидация не пройдена: описание пустое");
            throw new ValidationException("Описание не может быть пустым");
        }
    }
}
