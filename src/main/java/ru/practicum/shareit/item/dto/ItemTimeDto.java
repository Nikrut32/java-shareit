package ru.practicum.shareit.item.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ItemTimeDto implements ItemResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean available;
    private Long countRental;
    private LocalDate lastBooking;
    private LocalDate nextBooking;
    private List<CommentDto> comments;
}
