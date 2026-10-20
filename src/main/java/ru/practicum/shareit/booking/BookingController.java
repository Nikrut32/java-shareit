package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dal.mappers.BookingMapper;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.RequestStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingService;

import java.util.List;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final BookingService bookingService;

    @PostMapping
    public BookingDto create(@RequestBody BookingCreateDto booking, @RequestHeader("X-Sharer-User-Id") Long tenantId){
        return bookingService.createBooking(tenantId, booking);
    }

    @PatchMapping("/{bookingId}")
    public BookingDto updateStatus(@PathVariable Long bookingId,
                                           @RequestParam(name = "approved") boolean status,
                                           @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        return bookingService.UpdateStatus(status, bookingId, ownerId);
    }

    @GetMapping("/{bookingId}")
    public BookingDto getBookingById(@PathVariable Long bookingId, @RequestHeader("X-Sharer-User-Id") Long userId) {
        return  bookingService.getBookingById(bookingId, userId);
    }

    @GetMapping
    public List<BookingDto> getBookingsByBooker(@RequestHeader("X-Sharer-User-Id") Long bookerId,
                                                @RequestParam(name = "state", defaultValue = "ALL") RequestStatus status) {
        return bookingService.getBookingsByBookerId(bookerId, status);
    }

    @GetMapping("/owner")
    public List<BookingDto> getBookingsByOwner(@RequestHeader("X-Sharer-User-Id") Long ownerId,
                                                @RequestParam(name = "state", defaultValue = "ALL") RequestStatus status) {
        return bookingService.getBookingsByOwnerId(ownerId, status);
    }

}
