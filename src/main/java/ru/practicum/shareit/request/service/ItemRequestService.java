package ru.practicum.shareit.request.service;

import ru.practicum.shareit.request.dto.GetItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;

import java.util.List;

public interface ItemRequestService {
    ItemRequestDto createRequest (ItemRequest itemRequest, Long ownerId);

    List<GetItemRequestDto> getRequestsOwnerId(Long ownerId);

    List<ItemRequestDto> getRequestsAll(Long ownerId);

    GetItemRequestDto getRequestById(Long requestId);
}
