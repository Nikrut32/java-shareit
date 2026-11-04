package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.RequestStatus;
import ru.practicum.shareit.booking.service.BookingService;

import java.util.List;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public BookingDto create(@RequestBody BookingCreateDto booking, @RequestHeader("X-Sharer-User-Id") Long tenantId) {
        log.info("Получен запрос POST /bookings на создание бронирования: {}", booking);
        return bookingService.createBooking(tenantId, booking);
    }

    @PatchMapping("/{bookingId}")
    public BookingDto updateStatus(@PathVariable Long bookingId,
                                           @RequestParam(name = "approved") boolean status,
                                           @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        log.info("Получен запрос PATCH /bookings/{} на редактирование бронирования", bookingId);
        return bookingService.updateStatus(status, bookingId, ownerId);
    }

    @GetMapping("/{bookingId}")
    public BookingDto getBookingById(@PathVariable Long bookingId, @RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("Получен запрос GET /bookings/{} на вывод бронирования", bookingId);
        return  bookingService.getBookingById(bookingId, userId);
    }

    @GetMapping
    public List<BookingDto> getBookingsByBooker(@RequestHeader("X-Sharer-User-Id") Long bookerId,
                                                @RequestParam(name = "state", defaultValue = "ALL") RequestStatus status) {
        log.info("Получен запрос GET /bookings/{} на вывод бронирования для арендатора", bookerId);
        return bookingService.getBookingsByBookerId(bookerId, status);
    }

    @GetMapping("/owner")
    public List<BookingDto> getBookingsByOwner(@RequestHeader("X-Sharer-User-Id") Long ownerId,
                                                @RequestParam(name = "state", defaultValue = "ALL") RequestStatus status) {
        log.info("Получен запрос GET /bookings/{} на вывод бронирования для владельца", ownerId);
        return bookingService.getBookingsByOwnerId(ownerId, status);
    }

}
