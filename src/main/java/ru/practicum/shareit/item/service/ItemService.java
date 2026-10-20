package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.UpdateItemRequest;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.model.ItemResponse;

import java.util.List;

public interface ItemService {
    List<ItemDto> getAllItems(long ownerId);

    List<ItemDto> getItemSearchText(String searchText);

    public ItemResponse getItemById(long itemId, long ownerId);

    Item createItem(ItemDto itemDto, Long ownerId);

    ItemDto updateItem(UpdateItemRequest updateItem, long itemId, Long ownerId);

    void deleteItem(long itemId, Long ownerId);
}
