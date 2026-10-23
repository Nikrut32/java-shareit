package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.dal.mappers.BookingMapper;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.Status;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingServiceImpl;
import ru.practicum.shareit.exception.DataProcessingException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock private BookingMapper bookingMapper;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private ItemService itemService;
    @InjectMocks private BookingServiceImpl bookingService;

    private User owner;
    private Item item;
    private BookingCreateDto request;
    private Booking booking;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Owner").email("owner@example.com").build();
        item = Item.builder().id(10L).name("Drill").owner(owner).available(true).build();
        request = BookingCreateDto.builder().itemId(10L).start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2)).build();
        booking = Booking.builder().id(20L).booker(User.builder().id(2L).name("Booker").build())
                .item(item).start(request.getStart()).end(request.getEnd()).status(Status.WAITING).build();
    }

    @Test
    void createBookingSavesWaitingBookingAndIncreasesRentalCount() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(itemRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        when(bookingRepository.findAll()).thenReturn(List.of());
        when(bookingMapper.toEntityCreate(request, 2L)).thenReturn(booking);
        when(bookingRepository.save(booking)).thenReturn(booking);
        BookingDto dto = BookingDto.builder().id(20L).status(Status.WAITING).build();
        when(bookingMapper.toDto(booking)).thenReturn(dto);

        assertEquals(dto, bookingService.createBooking(2L, request));
        verify(itemService).increaseRental(10L);
        verify(bookingRepository).save(booking);
        assertEquals(Status.WAITING, booking.getStatus());
    }

    @Test
    void createBookingRejectsUnknownBooker() {
        when(userRepository.existsById(2L)).thenReturn(false);
        assertThrows(ValidationNotObjectException.class, () -> bookingService.createBooking(2L, request));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingRejectsUnavailableItem() {
        item.setAvailable(false);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(itemRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        assertThrows(ValidationException.class, () -> bookingService.createBooking(2L, request));
    }

    @Test
    void createBookingRejectsOwnerBookingOwnItem() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        assertThrows(DataProcessingException.class, () -> bookingService.createBooking(1L, request));
    }

    @Test
    void updateStatusApprovesBookingForOwner() {
        when(bookingRepository.existsById(20L)).thenReturn(true);
        when(bookingRepository.getReferenceById(20L)).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenAnswer(invocation -> BookingDto.builder()
                .id(booking.getId()).status(booking.getStatus()).build());

        BookingDto result = bookingService.updateStatus(true, 20L, 1L);

        assertEquals(Status.APPROVED, result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void updateStatusRejectsNonOwner() {
        when(bookingRepository.existsById(20L)).thenReturn(true);
        when(bookingRepository.getReferenceById(20L)).thenReturn(booking);
        assertThrows(ValidationException.class, () -> bookingService.updateStatus(true, 20L, 99L));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void getBookingByIdRejectsUserWhoIsNeitherBookerNorOwner() {
        when(bookingRepository.existsById(20L)).thenReturn(true);
        when(userRepository.existsById(99L)).thenReturn(true);
        when(bookingRepository.getReferenceById(20L)).thenReturn(booking);
        assertThrows(ValidationException.class, () -> bookingService.getBookingById(20L, 99L));
    }
}
