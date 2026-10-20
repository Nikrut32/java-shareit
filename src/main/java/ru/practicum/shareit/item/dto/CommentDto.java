package ru.practicum.shareit.item.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentDto {
    private Long id;
    private String authorName;
    private Item item;
    private String text;
    private LocalDateTime created;

    @Data
    @Builder
    public static class Item {
        private Long id;
        private String name;
    }
}
