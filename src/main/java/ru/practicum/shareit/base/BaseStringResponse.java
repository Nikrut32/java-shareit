package ru.practicum.shareit.base;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class BaseStringResponse {
    private final String response;
}
