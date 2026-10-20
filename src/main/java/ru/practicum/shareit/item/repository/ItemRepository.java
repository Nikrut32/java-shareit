package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;

import java.util.List;

public interface ItemRepository {
    List<Item> getAllItems(long ownerId);

    Item getItemById(Long itemId);

    Item createItem(ItemDto item, Long ownerId, Long requestId);

    ItemDto updateItem(ItemDto updateItem);

    void deleteItem(Long itemId);

    boolean checkOwner(long itemId, long ownerId);

    List<Item> getItemSearchText(String searchText);
}
