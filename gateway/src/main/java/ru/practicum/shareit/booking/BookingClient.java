package ru.practicum.shareit.booking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;

import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.exception.DataProcessingException;
import ru.practicum.shareit.exception.ValidationException;

import java.util.Map;

@Service
public class BookingClient extends BaseClient {
    private static final String API_PREFIX = "/bookings";

    @Autowired
    public BookingClient(@Value("${shareit-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
    }

    public ResponseEntity<Object> createBooking(long userId, BookItemRequestDto booking) {
        exceptionBooking(booking);
        return post("", userId, booking);
    }

    public ResponseEntity<Object> updateStatus(long bookingId, boolean status, Long userId) {
        Map<String, Object> parameters = Map.of(
                "approved", status
        );

        return patch("/" +  bookingId + "?approved={approved}", userId, parameters, null);
    }

    public ResponseEntity<Object> getBookingById(long bookingId, Long userId) {
        return get("/" +  bookingId, userId);
    }

    public ResponseEntity<Object> getBookingsByUserId(long userId, BookingState status, String path) {
        Map<String, Object> parameters = Map.of(
                "state", status
        );

        return get(path + "?state={state}", userId, parameters);
    }

    private void exceptionBooking(BookItemRequestDto booking) {
        if (booking.getStart() == null) {
            throw new DataProcessingException("Вы не указали время начала бронирования");
        }

        if (booking.getEnd() == null) {
            throw new DataProcessingException("Вы не указали время окончания бронирования");
        }

        if (booking.getStart().isAfter(booking.getEnd())) {
            throw new ValidationException("Время начала бронирования не может быть позже его окончания");
        }
    }
}
