package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.GetItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.util.List;

/**
 * TODO Sprint add-item-requests.
 */
@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
@Slf4j
public class ItemRequestController {
    private final ItemRequestService itemRequestService;

    @PostMapping
    public ItemRequestDto createItemRequest(@RequestBody ItemRequest itemRequest,
                                            @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.info("Получен запрос POST /requests на создание запроса: {}", itemRequest);
        return itemRequestService.createRequest(itemRequest, ownerId);
    }

    @GetMapping
    public List<GetItemRequestDto> getRequestsOwnerId(@RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.info("Получен запрос GET /requests, для пользователя с id={}", ownerId);
        return itemRequestService.getRequestsOwnerId(ownerId);
    }

    @GetMapping("/all")
    public List<ItemRequestDto> getRequests(@RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.info("Получен запрос GET /requests/all, для вывода всех запросов");
        return itemRequestService.getRequestsAll(ownerId);
    }

    @GetMapping("/{requestId}")
    public GetItemRequestDto getRequestById(@PathVariable("requestId") Long requestId) {
        log.info("Получен запрос GET /requests/{} на вывод пользователя", requestId);
        return itemRequestService.getRequestById(requestId);
    }
}
