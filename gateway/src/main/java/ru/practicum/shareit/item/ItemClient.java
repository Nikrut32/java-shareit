package ru.practicum.shareit.item;

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
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.Map;

@Service
@Slf4j
public class ItemClient extends BaseClient {
    private static final String API_PREFIX = "/items";

    @Autowired
    public ItemClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    public ResponseEntity<Object> getItems(long ownerId) {
        return get("", ownerId);
    }

    public ResponseEntity<Object> createItem(ItemDto itemDto, long ownerId) {
        exceptionItem(itemDto);

        return post("", ownerId, itemDto);
    }

    public ResponseEntity<Object> getItemById(long itemId, long ownerId) {
        return get("/" +  itemId, ownerId);
    }

    public ResponseEntity<Object> getItemText(String text, long userId) {
        Map<String, Object> parameters = Map.of(
                "text", text
        );

        return get("/search?text={text}", userId, parameters);
    }

    public ResponseEntity<Object> createComment(long userId, CommentCreateDto comment, long itemId) {
        return post("/" + itemId + "/comment", userId, comment);
    }

    public ResponseEntity<Object> updateItem(long userId, long itemId, ItemDto itemDto) {
        return patch("/" + itemId, userId, itemDto);
    }

    public ResponseEntity<Object> deleteItem(long itemId, long userId) {
        return delete("/" + itemId, userId);
    }

    private void exceptionItem(ItemDto item) {
        if (item.getName() == null) {
            log.warn("Валидация не пройдена: название не было указан");
            throw new ValidationException("Название не было указан");
        }
        if (item.getName().isBlank()) {
            log.warn("Валидация не пройдена: название пустое");
            throw new ValidationException("Название не может быть пустым");
        }
        if (item.getDescription() == null) {
            log.warn("Валидация не пройдена: email не был указан");
            throw new ValidationException("Email не был указан");
        }
        if (item.getDescription().isBlank()) {
            log.warn("Валидация не пройдена: email пустой");
            throw new ValidationException("Указан пустой email");
        }
        if (item.getAvailable() == null) {
            log.warn("Валидация не пройдена: статус доступа не был указан");
            throw new ValidationException("статус доступа не был указан");
        }
    }
}
