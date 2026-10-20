package ru.practicum.shareit.booking.dal.mappers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.repository.UserRepository;

@Component
public class BookingMapper {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    public BookingDto toDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        return BookingDto.builder()
                .id(booking.getId())
                .booker(BookingDto.Booker.builder()
                        .id(booking.getBooker().getId())
                        .name(booking.getBooker().getName())
                        .build())
                .item(BookingDto.Item.builder()
                        .id(booking.getItem().getId())
                        .name(booking.getItem().getName())
                        .build())
                .status(booking.getStatus())
                .start(booking.getStart())
                .end(booking.getEnd())
                .build();
    }

    public Booking toEntity(BookingDto bookingDto) {
        if (bookingDto == null) {
            return null;
        }

        return Booking.builder()
                .id(bookingDto.getId())
                .booker(userRepository.getReferenceById(bookingDto.getBooker().getId()))
                .item(itemRepository.getReferenceById(bookingDto.getItem().getId()))
                .status(bookingDto.getStatus())
                .start(bookingDto.getStart())
                .end(bookingDto.getEnd())
                .build();
    }

    public Booking toEntityCreate(BookingCreateDto bookingCreateDto, Long tenantId) {
        if (bookingCreateDto == null) {
            return null;
        }

        return Booking.builder()
                .item(itemRepository.findById(bookingCreateDto.getItemId())
                        .orElseThrow(() -> new ValidationNotObjectException("Предмет для бронирования не найден")))
                .booker(userRepository.findById(tenantId)
                        .orElseThrow(() -> new ValidationNotObjectException("Ваш пользователь не зарегистрирован")))
                .start(bookingCreateDto.getStart())
                .end(bookingCreateDto.getEnd())
                .build();
    }
}
