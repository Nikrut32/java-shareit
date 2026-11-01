package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.GetItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.util.List;

/**
 * TODO Sprint add-item-requests.
 */
@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private final ItemRequestService itemRequestService;

    @PostMapping
    public ItemRequestDto createItemRequest(@RequestBody ItemRequest itemRequest,
                                            @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        return itemRequestService.createRequest(itemRequest, ownerId);
    }

    @GetMapping
    public List<GetItemRequestDto> getRequestsOwnerId(@RequestHeader("X-Sharer-User-Id") Long ownerId) {
        return itemRequestService.getRequestsOwnerId(ownerId);
    }

    @GetMapping("/all")
    public List<ItemRequestDto> getRequests(@RequestHeader("X-Sharer-User-Id") Long ownerId) {
        return itemRequestService.getRequestsAll(ownerId);
    }

    @GetMapping("/{requestId}")
    public GetItemRequestDto getRequestById(@PathVariable("requestId") Long requestId) {
        return itemRequestService.getRequestById(requestId);
    }
}
