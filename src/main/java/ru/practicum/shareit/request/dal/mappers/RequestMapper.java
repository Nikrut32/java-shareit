package ru.practicum.shareit.request.dal.mappers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.dal.mappers.ItemMapper;
import ru.practicum.shareit.item.dto.RequestItemDto;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.GetItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.dal.mappers.UserMapper;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor()
public class RequestMapper {
    private final UserMapper userMapper;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Transactional
    public ItemRequestDto toDto (ItemRequest itemRequest) {
        return ItemRequestDto.builder()
                .id(itemRequest.getId())
                .description(itemRequest.getDescription())
                .created(itemRequest.getCreated())
                .owner(userMapper.toDto(itemRequest.getOwner()))
                .build();
    }

    @Transactional
    public ItemRequest toEntity (ItemRequestDto itemRequestDto) {
        if  (itemRequestDto == null) {
            return null;
        }

        return ItemRequest.builder()
                .id(itemRequestDto.getId())
                .description(itemRequestDto.getDescription())
                .created(itemRequestDto.getCreated())
                .owner(userMapper.toEntity(itemRequestDto.getOwner()))
                .build();
    }

    public GetItemRequestDto toGetRequestDto (ItemRequest itemRequest) {
        return GetItemRequestDto.builder()
                .id(itemRequest.getId())
                .description(itemRequest.getDescription())
                .created(itemRequest.getCreated())
                .items(itemRepository.findByRequestId(itemRequest.getId()).stream()
                        .map(itemMapper::toRequestDto)
                        .toList())
                .build();
    }
}
