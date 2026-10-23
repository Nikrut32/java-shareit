package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.RequestStatus;

import java.util.List;

public interface BookingService {
    BookingDto updateStatus(boolean status, Long bookingId, Long ownerId);

    BookingDto createBooking(Long tenantId, BookingCreateDto booking);

    BookingDto getBookingById(Long bookingId, Long userId);

    List<BookingDto> getBookingsByBookerId(Long bookerId, RequestStatus status);

    List<BookingDto> getBookingsByOwnerId(Long ownerId, RequestStatus status);
}
