package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.CreateRequestDto;

@Controller
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
@Slf4j
@Validated
public class RequestController {
    private final RequestClient requestClient;

    @PostMapping
    public ResponseEntity<Object> createRequest(@Valid @RequestBody CreateRequestDto createRequestDto,
                                                @RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("Получен запрос POST /requests на создание запроса: {}", createRequestDto);
        return requestClient.createRequest(createRequestDto, userId);
    }

    @GetMapping
    public ResponseEntity<Object> getRequestsOwnerId(@RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("Получен запрос GET /requests, для пользователя с id={}", userId);
        return requestClient.getRequests(userId, "");
    }

    @GetMapping("/all")
    public ResponseEntity<Object> getRequests(@RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("Получен запрос GET /requests/all, для вывода всех запросов");
        return requestClient.getRequests(userId, "/all");
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<Object> getRequestById(@PathVariable("requestId") Long requestId,
                                                 @RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("Получен запрос GET /requests/{} на вывод пользователя", requestId);
        return requestClient.getRequests(userId, "/" + requestId);
    }
}
