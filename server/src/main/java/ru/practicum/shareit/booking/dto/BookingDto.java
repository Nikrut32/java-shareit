package ru.practicum.shareit.booking.dto;

import lombok.Builder;
import lombok.Data;
import ru.practicum.shareit.booking.model.Status;

import java.time.LocalDateTime;

@Builder
@Data
public class BookingDto {
    private Long id;
    private Booker booker;
    private Item item;
    private Status status;
    private LocalDateTime start;
    private LocalDateTime end;

    @Data
    @Builder
    public static class Booker {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class Item {
        private Long id;
        private String name;
    }
}
