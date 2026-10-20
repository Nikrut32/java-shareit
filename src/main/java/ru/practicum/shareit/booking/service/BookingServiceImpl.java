package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dal.mappers.BookingMapper;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.RequestStatus;
import ru.practicum.shareit.booking.model.Status;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.DataProcessingException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingMapper bookingMapper;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;

    @Override
    public BookingDto UpdateStatus(boolean status, Long bookingId, Long ownerId) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new ValidationNotObjectException("Запрос на бронирование не был создан");
        }

        Booking booking = bookingRepository.getReferenceById(bookingId);

        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new ValidationException("Подтвердить бронирование может только владелец");
        }

        if (status) {
            booking.setStatus(Status.APPROVED);
            bookingRepository.save(booking);
        } else {
            booking.setStatus(Status.REJECTED);
            bookingRepository.save(booking);
        }

        return  bookingMapper.toDto(booking);
    }

    @Override
    public BookingDto createBooking(Long tenantId, BookingCreateDto bookingCreate) {
        checkBooking(tenantId, bookingCreate);
        Booking booking = bookingMapper.toEntityCreate(bookingCreate, tenantId);
        booking.setStatus(Status.WAITING);
        itemService.increaseRental(booking.getItem().getId());
        return bookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    public BookingDto getBookingById(Long bookingId, Long userId) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new ValidationNotObjectException("Такого предмета не существует");
        }

        if (!userRepository.existsById(userId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }

        if (!bookingRepository.getReferenceById(bookingId).getBooker().getId().equals(userId)
                && !bookingRepository.getReferenceById(bookingId).getItem().getOwner().getId().equals(userId)) {
            throw new ValidationException("Просмотреть детали бронирования может только автор или владелец предмета");
        }

        return  bookingMapper.toDto(bookingRepository.getReferenceById(bookingId));
    }

    @Override
    public List<BookingDto> getBookingsByBookerId(Long bookerId, RequestStatus status) {
        if (!userRepository.existsById(bookerId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }

        List<BookingDto> bookings = bookingRepository.findAll().stream()
                .filter(booking -> booking.getBooker().getId().equals(bookerId))
                .map(bookingMapper::toDto)
                .sorted(Comparator.comparing(BookingDto::getStart))
                .toList();

        return checkStatus(bookings, status);
    }

    @Override
    public List<BookingDto> getBookingsByOwnerId(Long ownerId, RequestStatus status) {
        if (!userRepository.existsById(ownerId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }

        List<BookingDto> bookings = bookingRepository.findAll().stream()
                .filter(booking -> booking.getItem().getOwner().getId().equals(ownerId))
                .map(bookingMapper::toDto)
                .sorted(Comparator.comparing(BookingDto::getStart))
                .toList();

        if (bookings.isEmpty()) {
            throw new ValidationNotObjectException("У вас нет предметов для бронирования");
        }

        return checkStatus(bookings, status);
    }

    private List<BookingDto> checkStatus(List<BookingDto> bookings, RequestStatus status) {
        return switch (status) {
            case ALL -> bookings;
            case CURRENT -> bookings.stream()
                    .filter(booking -> booking.getStart().isBefore(LocalDateTime.now())
                            && booking.getEnd().isAfter(LocalDateTime.now()))
                    .toList();
            case PAST -> bookings.stream()
                    .filter(booking -> booking.getEnd().isBefore(LocalDateTime.now()))
                    .toList();
            case FUTURE -> bookings.stream()
                    .filter(booking -> booking.getStart().isAfter(LocalDateTime.now()))
                    .toList();
            case WAITING -> bookings.stream()
                    .filter(booking -> booking.getStatus() == Status.WAITING)
                    .toList();
            case REJECTED -> bookings.stream()
                    .filter(booking -> booking.getStatus() == Status.REJECTED)
                    .toList();
        };
    }

    private void checkBooking(Long tenantId, BookingCreateDto bookingCreate) {
        if (!userRepository.existsById(tenantId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }

        if (!itemRepository.existsById(bookingCreate.getItemId())) {
            throw new ValidationNotObjectException("Предмет не найден");
        }

        if (!itemRepository.getReferenceById(bookingCreate.getItemId()).getAvailable()) {
            throw new ValidationException("Этот предмет сейчас нельзя забронировать");
        }

        if (itemRepository.getReferenceById(bookingCreate.getItemId()).getOwner().getId().equals(tenantId)) {
            throw new DataProcessingException("Вы являетесь владельцем этого предмета и не можете его забронировать");
        }

        if (bookingCreate.getStart() == null) {
            throw new DataProcessingException("Вы не указали время начала бронирования");
        }

        if (bookingCreate.getEnd() == null) {
            throw new DataProcessingException("Вы не указали время окончания бронирования");
        }

        if (bookingCreate.getStart().isAfter(bookingCreate.getEnd())) {
            throw new ValidationException("Время начала бронирования не может быть позже его окончания");
        }

        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(booking -> booking.getItem().getId().equals(bookingCreate.getItemId()))
                .filter(booking -> booking.getStatus() != Status.REJECTED)
                .toList();

        for (Booking booking : bookings) {
            if (bookingCreate.getStart().isAfter(booking.getStart())
                    && bookingCreate.getStart().isBefore(booking.getEnd())) {
                throw new DataProcessingException("Этот предмет уже забронирован на ваше время");
            }
        }
    }
}
