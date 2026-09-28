package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.base.BaseStringResponse;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.UpdateItemRequest;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.model.ItemResponse;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {
    private final ItemService itemService;

    @GetMapping
    public List<ItemDto> getItems(@RequestHeader("X-Sharer-User-Id") long ownerId) {
        log.info("Получен запрос GET /items, для пользователя с id={}", ownerId);
        return itemService.getAllItems(ownerId);
    }

    @GetMapping("/{id}")
    public ItemResponse getItem(@PathVariable long id, @RequestHeader("X-Sharer-User-Id") long ownerId) {
        log.info("Получен запрос GET /items/{}, для пользователя с id={}", id, ownerId);
        return itemService.getItemById(id, ownerId);
    }

    @GetMapping("/search")
    public List<ItemDto> getItemSearchText(@RequestParam String text) {
        log.info("Получен запрос GET /items/search, для поиска с text={}", text);
        return  itemService.getItemSearchText(text);
    }

    @PostMapping
    public Item createItem(@RequestHeader("X-Sharer-User-Id") long ownerId, @RequestBody ItemDto itemDto) {
        log.info("Получен запрос POST /items, от пользователя с id={}", ownerId);
        return itemService.createItem(itemDto, ownerId);
    }

    @PatchMapping("/{id}")
    public ItemDto updateItem(@PathVariable long id,
                              @RequestHeader("X-Sharer-User-Id") long ownerId,
                              @RequestBody UpdateItemRequest updateItem) {
        log.info("Получен запрос PATCH /items/{}, от пользователя с id={}", id, ownerId);
        return itemService.updateItem(updateItem, id, ownerId);
    }

    @DeleteMapping("/{id}")
    public BaseStringResponse deleteItem(@PathVariable long id,
                                         @RequestHeader("X-Sharer-User-Id") long ownerId) {
        log.info("Получен запрос DELETE /items/{}, от пользователя с id={}", id, ownerId);
        itemService.deleteItem(id, ownerId);
        return new BaseStringResponse("Предмет с id: " + id + " успешно удален");
    }

}
