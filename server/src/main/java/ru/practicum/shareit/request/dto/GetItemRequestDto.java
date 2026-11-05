package ru.practicum.shareit.request.dto;

import lombok.Builder;
import lombok.Data;
import ru.practicum.shareit.item.dto.RequestItemDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GetItemRequestDto {
    private Long id;
    private String description;
    private LocalDateTime created;
    private List<RequestItemDto> items;
}
