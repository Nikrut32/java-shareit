package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;


@Controller
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
@Slf4j
@Validated
public class BookingController {
	private final BookingClient bookingClient;

	@PostMapping
	public ResponseEntity<Object> createBooking(@Valid @RequestBody BookItemRequestDto booking,
												@RequestHeader("X-Sharer-User-Id") Long userId) {
		log.info("Получен запрос POST /bookings на создание бронирования: {}", booking);
		return bookingClient.createBooking(userId, booking);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<Object> updateStatus(@PathVariable Long id,
	                                           @RequestParam(name = "approved") boolean status,
	                                           @RequestHeader("X-Sharer-User-Id") Long userId) {
		log.info("Получен запрос PATCH /bookings/{} на редактирование бронирования", id);
		return bookingClient.updateStatus(id, status, userId);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Object> getBookingById(@PathVariable Long id, @RequestHeader("X-Sharer-User-Id") Long userId) {
		log.info("Получен запрос GET /bookings/{} на вывод бронирования", id);
		return bookingClient.getBookingById(id, userId);
	}

	@GetMapping
	public ResponseEntity<Object> getBookingsByBooker(@RequestHeader("X-Sharer-User-Id") Long userId,
	                                            @RequestParam(name = "state", defaultValue = "ALL") BookingState status) {
		log.info("Получен запрос GET /bookings/{} на вывод бронирования для арендатора", userId);
		return bookingClient.getBookingsByUserId(userId, status, "");
	}

	@GetMapping("/owner")
	public ResponseEntity<Object> getBookingsByOwner(@RequestHeader("X-Sharer-User-Id") Long userId,
	                                           @RequestParam(name = "state", defaultValue = "ALL") BookingState status) {
		log.info("Получен запрос GET /bookings/{} на вывод бронирования для владельца", userId);
		return bookingClient.getBookingsByUserId(userId, status, "/owner");
	}
}
