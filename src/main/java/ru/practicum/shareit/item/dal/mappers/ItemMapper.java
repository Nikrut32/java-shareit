package ru.practicum.shareit.item.dal.mappers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemTimeDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.dal.mappers.UserMapper;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemMapper {
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Transactional
    public ItemDto mapToDto(Item item) {
        if  (item == null) {
            return null;
        }

        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .countRental(item.getCountRental())
                .comments(item.getComments().stream().map(commentMapper::toDto).toList())
                .build();
    }

    @Transactional
    public ItemCreateDto mapToCreateDto(Item item) {
        if (item == null) {
            return null;
        }

        return ItemCreateDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .owner(userMapper.toDto(item.getOwner()))
                .countRental(item.getCountRental())
                .request(item.getRequest())
                .build();
    }

    @Transactional
    public ItemTimeDto mapToTimeDto(Item item) {
        LocalDate now = LocalDate.now();
        LocalDate nextTime = null;
        LocalDate lastTime = null;;
        List<Booking> bookings = bookingRepository.findAll();

        List<LocalDate> itemBookingTimeStart = bookings.stream()
                .filter(itemBooking -> itemBooking.getItem().getId().equals(item.getId()))
                .map(Booking::getStart)
                .map(LocalDateTime::toLocalDate)
                .sorted()
                .toList();

        List<LocalDate> itemBookingTimeEnd = bookings.stream()
                .filter(itemBooking -> itemBooking.getItem().getId().equals(item.getId()))
                .map(Booking::getEnd)
                .map(LocalDateTime::toLocalDate)
                .sorted(Comparator.reverseOrder())
                .toList();

        for (LocalDate start : itemBookingTimeStart) {
            if (start.isAfter(now)) {
                nextTime = start;
                break;
            }
        }

        for (LocalDate end : itemBookingTimeEnd) {
            if (end.isBefore(now)) {
                lastTime = end;
                break;
            }
        }

        return ItemTimeDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .countRental(item.getCountRental())
                .nextBooking(nextTime)
                .lastBooking(lastTime)
                .comments(item.getComments().stream().map(commentMapper::toDto).toList())
                .build();
    }

    @Transactional
    public Item toEntity(ItemDto dto, long ownerId, Long requestId) {
        if  (dto == null) {
            return null;
        }

        return Item.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .available(dto.getAvailable())
                .countRental(dto.getCountRental())
                .owner(userRepository.getReferenceById(ownerId))
                .request(null)
                .build();
    }
}
