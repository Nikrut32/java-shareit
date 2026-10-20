package ru.practicum.shareit.item.dto;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ItemDto implements ItemResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean available;
    private Long countRental;
    private List<CommentDto> comments;
}
