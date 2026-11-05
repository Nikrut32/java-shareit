package ru.practicum.shareit.item.dto;

import lombok.Data;

@Data
public class UpdateItemRequest {
    private String name;
    private String description;
    private Boolean available;

    public boolean hasDescription() {
        return !(description == null || description.isBlank());
    }

    public boolean hasName() {
        return !(name == null || name.isBlank());
    }

    public boolean hasAvailable() {
        return !(available == null);
    }
}
