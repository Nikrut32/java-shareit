package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;

@Controller
@RequestMapping(path = "/items")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ItemController {
    private final ItemClient itemClient;

    @GetMapping
    public ResponseEntity<Object> getItems(@RequestHeader("X-Sharer-User-Id") long userId) {
        log.info("Получен запрос GET /items, для пользователя с id={}", userId);
        return itemClient.getItems(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getItemById(@PathVariable long id, @RequestHeader("X-Sharer-User-Id") long userId) {
        log.info("Получен запрос GET /items/{}, для пользователя с id={}", id, userId);
        return itemClient.getItemById(id, userId);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> getItemSearchText(@RequestParam String text,
                                                    @RequestHeader("X-Sharer-User-Id") long userId) {
        log.info("Получен запрос GET /items/search, для поиска с text={}", text);
        return itemClient.getItemText(text, userId);
    }

    @PostMapping
    public ResponseEntity<Object> createItem(@Validated @RequestBody ItemDto itemDto,
                                             @RequestHeader("X-Sharer-User-Id") long userId) {
        log.info("Получен запрос POST /items, от пользователя с id={}", userId);
        return itemClient.createItem(itemDto, userId);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<Object> createComment(@RequestHeader("X-Sharer-User-Id") long userId,
                                                @RequestBody CommentCreateDto comment,
                                                @PathVariable long itemId) {
        log.info("Получен запрос POST /items/{}/comment, от пользователя с id={}", itemId, userId);
        return itemClient.createComment(userId, comment, itemId);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Object> updateItem(@PathVariable long id,
                                             @RequestHeader("X-Sharer-User-Id") long userId,
                                             @RequestBody ItemDto updateItem) {
        log.info("Получен запрос PATCH /items/{}, от пользователя с id={}", id, userId);
        return itemClient.updateItem(id, userId, updateItem);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteItem(@PathVariable long id, @RequestHeader("X-Sharer-User-Id") long userId) {
        log.info("Получен запрос DELETE /items/{}, от пользователя с id={}", id, userId);
        return itemClient.deleteItem(id, userId);
    }
}
