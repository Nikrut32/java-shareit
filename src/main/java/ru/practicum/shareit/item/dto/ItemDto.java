package ru.practicum.shareit.item.dto;

import lombok.Builder;
import lombok.Data;
import ru.practicum.shareit.item.model.ItemResponse;

@Data
@Builder
public class ItemDto implements ItemResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean available;
    private Long countRental;
}
