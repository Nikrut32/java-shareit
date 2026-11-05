package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.dal.mappers.BookingMapper;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.RequestStatus;
import ru.practicum.shareit.booking.model.Status;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingTest {
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ItemService itemService;
    @InjectMocks
    private BookingServiceImpl bookingService;

    private BookingController bookingController;
    private User owner;
    private User booker;
    private Item item;
    private BookingCreateDto request;
    private Booking booking;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Owner").email("owner@example.com").build();
        booker = User.builder().id(2L).name("Booker").build();
        item = Item.builder().id(10L).name("Drill").owner(owner).available(true).build();
        request = BookingCreateDto.builder().itemId(10L)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2)).build();
        booking = Booking.builder().id(20L).booker(booker).item(item)
                .start(request.getStart()).end(request.getEnd()).status(Status.WAITING).build();
        bookingController = new BookingController(bookingService);
    }

    @Test
    void createSavesWaitingIncrementsRentalTest() {
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
    void createUnknownBookerThrowsTest() {
        when(userRepository.existsById(2L)).thenReturn(false);
        assertThrows(ValidationNotObjectException.class, () -> bookingService.createBooking(2L, request));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createUnknownItemThrowsTest() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(itemRepository.existsById(10L)).thenReturn(false);
        assertThrows(ValidationNotObjectException.class, () -> bookingService.createBooking(2L, request));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createUnavailableItemThrowsTest() {
        item.setAvailable(false);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(itemRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        assertThrows(ValidationException.class, () -> bookingService.createBooking(2L, request));
    }

    @Test
    void createOwnItemThrowsTest() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        assertThrows(DataProcessingException.class, () -> bookingService.createBooking(1L, request));
    }

    @Test
    void createOverlapThrowsTest() {
        Booking existing = Booking.builder()
                .id(99L).item(item).booker(booker)
                .start(request.getStart().minusHours(1))
                .end(request.getStart().plusHours(1))
                .status(Status.APPROVED).build();

        when(userRepository.existsById(2L)).thenReturn(true);
        when(itemRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        when(bookingRepository.findAll()).thenReturn(List.of(existing));

        assertThrows(DataProcessingException.class, () -> bookingService.createBooking(2L, request));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createOverlapSkipsRejectedTest() {
        Booking rejected = Booking.builder()
                .id(99L).item(item).booker(booker)
                .start(request.getStart().minusHours(1))
                .end(request.getStart().plusHours(1))
                .status(Status.REJECTED).build();

        when(userRepository.existsById(2L)).thenReturn(true);
        when(itemRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        when(bookingRepository.findAll()).thenReturn(List.of(rejected));
        when(bookingMapper.toEntityCreate(request, 2L)).thenReturn(booking);
        when(bookingRepository.save(booking)).thenReturn(booking);
        BookingDto dto = BookingDto.builder().id(20L).status(Status.WAITING).build();
        when(bookingMapper.toDto(booking)).thenReturn(dto);

        assertEquals(dto, bookingService.createBooking(2L, request));
    }

    @Test
    void approveByOwnerTest() {
        when(bookingRepository.existsById(20L)).thenReturn(true);
        when(bookingRepository.getReferenceById(20L)).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenAnswer(inv -> BookingDto.builder()
                .id(booking.getId()).status(booking.getStatus()).build());

        BookingDto result = bookingService.updateStatus(true, 20L, 1L);

        assertEquals(Status.APPROVED, result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void rejectByOwnerTest() {
        when(bookingRepository.existsById(20L)).thenReturn(true);
        when(bookingRepository.getReferenceById(20L)).thenReturn(booking);
        when(bookingMapper.toDto(booking)).thenAnswer(inv -> BookingDto.builder()
                .id(booking.getId()).status(booking.getStatus()).build());

        BookingDto result = bookingService.updateStatus(false, 20L, 1L);

        assertEquals(Status.REJECTED, result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void approveNonOwnerThrowsTest() {
        when(bookingRepository.existsById(20L)).thenReturn(true);
        when(bookingRepository.getReferenceById(20L)).thenReturn(booking);
        assertThrows(ValidationException.class, () -> bookingService.updateStatus(true, 20L, 99L));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approveUnknownBookingThrowsTest() {
        when(bookingRepository.existsById(20L)).thenReturn(false);
        assertThrows(ValidationNotObjectException.class, () -> bookingService.updateStatus(true, 20L, 1L));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void getByIdForBookerTest() {
        when(bookingRepository.existsById(20L)).thenReturn(true);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(bookingRepository.getReferenceById(20L)).thenReturn(booking);
        BookingDto dto = BookingDto.builder().id(20L).build();
        when(bookingMapper.toDto(booking)).thenReturn(dto);

        assertEquals(dto, bookingService.getBookingById(20L, 2L));
    }

    @Test
    void getByIdForOwnerTest() {
        when(bookingRepository.existsById(20L)).thenReturn(true);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(bookingRepository.getReferenceById(20L)).thenReturn(booking);
        BookingDto dto = BookingDto.builder().id(20L).build();
        when(bookingMapper.toDto(booking)).thenReturn(dto);

        assertEquals(dto, bookingService.getBookingById(20L, 1L));
    }

    @Test
    void getByIdUnknownBookingThrowsTest() {
        when(bookingRepository.existsById(20L)).thenReturn(false);
        assertThrows(ValidationNotObjectException.class, () -> bookingService.getBookingById(20L, 2L));
    }

    @Test
    void getByIdUnknownUserThrowsTest() {
        when(bookingRepository.existsById(20L)).thenReturn(true);
        when(userRepository.existsById(99L)).thenReturn(false);
        assertThrows(ValidationNotObjectException.class, () -> bookingService.getBookingById(20L, 99L));
    }

    @Test
    void getByIdStrangerThrowsTest() {
        when(bookingRepository.existsById(20L)).thenReturn(true);
        when(userRepository.existsById(99L)).thenReturn(true);
        when(bookingRepository.getReferenceById(20L)).thenReturn(booking);
        assertThrows(ValidationException.class, () -> bookingService.getBookingById(20L, 99L));
    }

    @Test
    void bookerUnknownUserThrowsTest() {
        when(userRepository.existsById(2L)).thenReturn(false);
        assertThrows(ValidationNotObjectException.class,
                () -> bookingService.getBookingsByBookerId(2L, RequestStatus.ALL));
    }

    @Test
    void bookerAllSortedTest() {
        Booking later = Booking.builder().id(21L).booker(booker).item(item)
                .start(LocalDateTime.now().plusDays(5)).end(LocalDateTime.now().plusDays(6))
                .status(Status.WAITING).build();
        Booking earlier = booking;

        when(userRepository.existsById(2L)).thenReturn(true);
        when(bookingRepository.findAll()).thenReturn(List.of(later, earlier));
        when(bookingMapper.toDto(later)).thenReturn(BookingDto.builder()
                .id(21L).start(later.getStart()).end(later.getEnd()).status(later.getStatus()).build());
        when(bookingMapper.toDto(earlier)).thenReturn(BookingDto.builder()
                .id(20L).start(earlier.getStart()).end(earlier.getEnd()).status(earlier.getStatus()).build());

        List<BookingDto> result = bookingService.getBookingsByBookerId(2L, RequestStatus.ALL);

        assertEquals(2, result.size());
        assertEquals(20L, result.get(0).getId());
        assertEquals(21L, result.get(1).getId());
    }

    @Test
    void bookerCurrentTest() {
        LocalDateTime now = LocalDateTime.now();
        Booking current = Booking.builder().id(1L).booker(booker).item(item)
                .start(now.minusHours(1)).end(now.plusHours(1)).status(Status.APPROVED).build();
        Booking past = Booking.builder().id(2L).booker(booker).item(item)
                .start(now.minusDays(2)).end(now.minusDays(1)).status(Status.APPROVED).build();

        when(userRepository.existsById(2L)).thenReturn(true);
        when(bookingRepository.findAll()).thenReturn(List.of(current, past));
        when(bookingMapper.toDto(current)).thenReturn(BookingDto.builder()
                .id(1L).start(current.getStart()).end(current.getEnd()).status(current.getStatus()).build());
        when(bookingMapper.toDto(past)).thenReturn(BookingDto.builder()
                .id(2L).start(past.getStart()).end(past.getEnd()).status(past.getStatus()).build());

        List<BookingDto> result = bookingService.getBookingsByBookerId(2L, RequestStatus.CURRENT);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void bookerPastTest() {
        LocalDateTime now = LocalDateTime.now();
        Booking past = Booking.builder().id(2L).booker(booker).item(item)
                .start(now.minusDays(2)).end(now.minusDays(1)).status(Status.APPROVED).build();
        Booking future = Booking.builder().id(3L).booker(booker).item(item)
                .start(now.plusDays(1)).end(now.plusDays(2)).status(Status.WAITING).build();

        when(userRepository.existsById(2L)).thenReturn(true);
        when(bookingRepository.findAll()).thenReturn(List.of(past, future));
        when(bookingMapper.toDto(past)).thenReturn(BookingDto.builder()
                .id(2L).start(past.getStart()).end(past.getEnd()).status(past.getStatus()).build());
        when(bookingMapper.toDto(future)).thenReturn(BookingDto.builder()
                .id(3L).start(future.getStart()).end(future.getEnd()).status(future.getStatus()).build());

        List<BookingDto> result = bookingService.getBookingsByBookerId(2L, RequestStatus.PAST);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void bookerFutureTest() {
        LocalDateTime now = LocalDateTime.now();
        Booking future = Booking.builder().id(3L).booker(booker).item(item)
                .start(now.plusDays(1)).end(now.plusDays(2)).status(Status.WAITING).build();
        Booking current = Booking.builder().id(1L).booker(booker).item(item)
                .start(now.minusHours(1)).end(now.plusHours(1)).status(Status.APPROVED).build();

        when(userRepository.existsById(2L)).thenReturn(true);
        when(bookingRepository.findAll()).thenReturn(List.of(future, current));
        when(bookingMapper.toDto(future)).thenReturn(BookingDto.builder()
                .id(3L).start(future.getStart()).end(future.getEnd()).status(future.getStatus()).build());
        when(bookingMapper.toDto(current)).thenReturn(BookingDto.builder()
                .id(1L).start(current.getStart()).end(current.getEnd()).status(current.getStatus()).build());

        List<BookingDto> result = bookingService.getBookingsByBookerId(2L, RequestStatus.FUTURE);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
    }

    @Test
    void bookerWaitingTest() {
        Booking waiting = Booking.builder().id(1L).booker(booker).item(item)
                .start(LocalDateTime.now().plusDays(1)).end(LocalDateTime.now().plusDays(2))
                .status(Status.WAITING).build();
        Booking approved = Booking.builder().id(2L).booker(booker).item(item)
                .start(LocalDateTime.now().plusDays(3)).end(LocalDateTime.now().plusDays(4))
                .status(Status.APPROVED).build();

        when(userRepository.existsById(2L)).thenReturn(true);
        when(bookingRepository.findAll()).thenReturn(List.of(waiting, approved));
        when(bookingMapper.toDto(waiting)).thenReturn(BookingDto.builder()
                .id(1L).start(waiting.getStart()).end(waiting.getEnd()).status(waiting.getStatus()).build());
        when(bookingMapper.toDto(approved)).thenReturn(BookingDto.builder()
                .id(2L).start(approved.getStart()).end(approved.getEnd()).status(approved.getStatus()).build());

        List<BookingDto> result = bookingService.getBookingsByBookerId(2L, RequestStatus.WAITING);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void bookerRejectedTest() {
        Booking rejected = Booking.builder().id(1L).booker(booker).item(item)
                .start(LocalDateTime.now().plusDays(1)).end(LocalDateTime.now().plusDays(2))
                .status(Status.REJECTED).build();
        Booking waiting = Booking.builder().id(2L).booker(booker).item(item)
                .start(LocalDateTime.now().plusDays(3)).end(LocalDateTime.now().plusDays(4))
                .status(Status.WAITING).build();

        when(userRepository.existsById(2L)).thenReturn(true);
        when(bookingRepository.findAll()).thenReturn(List.of(rejected, waiting));
        when(bookingMapper.toDto(rejected)).thenReturn(BookingDto.builder()
                .id(1L).start(rejected.getStart()).end(rejected.getEnd()).status(rejected.getStatus()).build());
        when(bookingMapper.toDto(waiting)).thenReturn(BookingDto.builder()
                .id(2L).start(waiting.getStart()).end(waiting.getEnd()).status(waiting.getStatus()).build());

        List<BookingDto> result = bookingService.getBookingsByBookerId(2L, RequestStatus.REJECTED);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void bookerEmptyTest() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(bookingRepository.findAll()).thenReturn(List.of());

        List<BookingDto> result = bookingService.getBookingsByBookerId(2L, RequestStatus.ALL);

        assertTrue(result.isEmpty());
    }

    @Test
    void ownerUnknownUserThrowsTest() {
        when(userRepository.existsById(1L)).thenReturn(false);
        assertThrows(ValidationNotObjectException.class,
                () -> bookingService.getBookingsByOwnerId(1L, RequestStatus.ALL));
    }

    @Test
    void ownerNoItemsThrowsTest() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(bookingRepository.findAll()).thenReturn(List.of());

        assertThrows(ValidationNotObjectException.class,
                () -> bookingService.getBookingsByOwnerId(1L, RequestStatus.ALL));
    }

    @Test
    void ownerAllTest() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(bookingRepository.findAll()).thenReturn(List.of(booking));
        when(bookingMapper.toDto(booking)).thenReturn(BookingDto.builder()
                .id(20L).start(booking.getStart()).end(booking.getEnd()).status(booking.getStatus()).build());

        List<BookingDto> result = bookingService.getBookingsByOwnerId(1L, RequestStatus.ALL);

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).getId());
    }

    @Test
    void ownerWaitingTest() {
        Booking waiting = booking;
        Booking rejected = Booking.builder().id(21L).booker(booker).item(item)
                .start(LocalDateTime.now().plusDays(3)).end(LocalDateTime.now().plusDays(4))
                .status(Status.REJECTED).build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(bookingRepository.findAll()).thenReturn(List.of(waiting, rejected));
        when(bookingMapper.toDto(waiting)).thenReturn(BookingDto.builder()
                .id(20L).start(waiting.getStart()).end(waiting.getEnd()).status(waiting.getStatus()).build());
        when(bookingMapper.toDto(rejected)).thenReturn(BookingDto.builder()
                .id(21L).start(rejected.getStart()).end(rejected.getEnd()).status(rejected.getStatus()).build());

        List<BookingDto> result = bookingService.getBookingsByOwnerId(1L, RequestStatus.WAITING);

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).getId());
    }

    @Test
    void ownerSortedTest() {
        Booking later = Booking.builder().id(21L).booker(booker).item(item)
                .start(LocalDateTime.now().plusDays(5)).end(LocalDateTime.now().plusDays(6))
                .status(Status.WAITING).build();
        Booking earlier = booking;

        when(userRepository.existsById(1L)).thenReturn(true);
        when(bookingRepository.findAll()).thenReturn(List.of(later, earlier));
        when(bookingMapper.toDto(later)).thenReturn(BookingDto.builder()
                .id(21L).start(later.getStart()).end(later.getEnd()).status(later.getStatus()).build());
        when(bookingMapper.toDto(earlier)).thenReturn(BookingDto.builder()
                .id(20L).start(earlier.getStart()).end(earlier.getEnd()).status(earlier.getStatus()).build());

        List<BookingDto> result = bookingService.getBookingsByOwnerId(1L, RequestStatus.ALL);

        assertEquals(2, result.size());
        assertEquals(20L, result.get(0).getId());
        assertEquals(21L, result.get(1).getId());
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class BookingControllerTest {

        @Mock
        private BookingService bookingService;
        private BookingController bookingController;

        private BookingCreateDto request;

        @BeforeEach
        void setUp() {
            bookingController = new BookingController(bookingService);
            request = BookingCreateDto.builder()
                    .itemId(10L)
                    .start(LocalDateTime.now().plusDays(1))
                    .end(LocalDateTime.now().plusDays(2))
                    .build();
        }

        @Test
        void createDelegatesToServiceTest() {
            BookingDto expected = BookingDto.builder().id(20L).status(Status.WAITING).build();
            when(bookingService.createBooking(2L, request)).thenReturn(expected);

            assertEquals(expected, bookingController.create(request, 2L));
            verify(bookingService).createBooking(2L, request);
        }

        @Test
        void updateStatusApproveDelegatesTest() {
            BookingDto expected = BookingDto.builder().id(20L).status(Status.APPROVED).build();
            when(bookingService.updateStatus(true, 20L, 1L)).thenReturn(expected);

            assertEquals(expected, bookingController.updateStatus(20L, true, 1L));
            verify(bookingService).updateStatus(true, 20L, 1L);
        }

        @Test
        void updateStatusRejectDelegatesTest() {
            BookingDto expected = BookingDto.builder().id(20L).status(Status.REJECTED).build();
            when(bookingService.updateStatus(false, 20L, 1L)).thenReturn(expected);

            assertEquals(expected, bookingController.updateStatus(20L, false, 1L));
            verify(bookingService).updateStatus(false, 20L, 1L);
        }

        @Test
        void getBookingByIdDelegatesTest() {
            BookingDto expected = BookingDto.builder().id(20L).build();
            when(bookingService.getBookingById(20L, 2L)).thenReturn(expected);

            assertEquals(expected, bookingController.getBookingById(20L, 2L));
            verify(bookingService).getBookingById(20L, 2L);
        }

        @Test
        void getBookingsByBookerAllDelegatesTest() {
            List<BookingDto> expected = List.of(BookingDto.builder().id(20L).build());
            when(bookingService.getBookingsByBookerId(2L, RequestStatus.ALL)).thenReturn(expected);

            assertEquals(expected, bookingController.getBookingsByBooker(2L, RequestStatus.ALL));
            verify(bookingService).getBookingsByBookerId(2L, RequestStatus.ALL);
        }

        @Test
        void getBookingsByBookerWaitingDelegatesTest() {
            List<BookingDto> expected = List.of(BookingDto.builder().id(20L).status(Status.WAITING).build());
            when(bookingService.getBookingsByBookerId(2L, RequestStatus.WAITING)).thenReturn(expected);

            assertEquals(expected, bookingController.getBookingsByBooker(2L, RequestStatus.WAITING));
            verify(bookingService).getBookingsByBookerId(2L, RequestStatus.WAITING);
        }

        @Test
        void getBookingsByBookerEmptyListTest() {
            when(bookingService.getBookingsByBookerId(2L, RequestStatus.ALL)).thenReturn(List.of());

            List<BookingDto> result = bookingController.getBookingsByBooker(2L, RequestStatus.ALL);

            assertTrue(result.isEmpty());
            verify(bookingService).getBookingsByBookerId(2L, RequestStatus.ALL);
        }

        @Test
        void getBookingsByOwnerAllDelegatesTest() {
            List<BookingDto> expected = List.of(BookingDto.builder().id(20L).build());
            when(bookingService.getBookingsByOwnerId(1L, RequestStatus.ALL)).thenReturn(expected);

            assertEquals(expected, bookingController.getBookingsByOwner(1L, RequestStatus.ALL));
            verify(bookingService).getBookingsByOwnerId(1L, RequestStatus.ALL);
        }

        @Test
        void getBookingsByOwnerRejectedDelegatesTest() {
            List<BookingDto> expected = List.of(BookingDto.builder().id(20L).status(Status.REJECTED).build());
            when(bookingService.getBookingsByOwnerId(1L, RequestStatus.REJECTED)).thenReturn(expected);

            assertEquals(expected, bookingController.getBookingsByOwner(1L, RequestStatus.REJECTED));
            verify(bookingService).getBookingsByOwnerId(1L, RequestStatus.REJECTED);
        }

        @Test
        void getBookingsByOwnerEmptyListTest() {
            when(bookingService.getBookingsByOwnerId(1L, RequestStatus.ALL)).thenReturn(List.of());

            List<BookingDto> result = bookingController.getBookingsByOwner(1L, RequestStatus.ALL);

            assertTrue(result.isEmpty());
            verify(bookingService).getBookingsByOwnerId(1L, RequestStatus.ALL);
        }
    }

    @Nested
    class MapperTests {

        private BookingMapper realBookingMapper;

        @BeforeEach
        void setUpBookingMapperTest() {
            realBookingMapper = new BookingMapper();
            ReflectionTestUtils.setField(realBookingMapper, "userRepository", userRepository);
            ReflectionTestUtils.setField(realBookingMapper, "itemRepository", itemRepository);
        }

        @Test
        void toDtoNullTest() {
            assertNull(realBookingMapper.toDto(null));
        }

        @Test
        void toDtoAllFieldsTest() {
            LocalDateTime start = LocalDateTime.of(2024, 5, 1, 10, 0);
            LocalDateTime end = LocalDateTime.of(2024, 5, 2, 12, 0);
            Booking source = Booking.builder()
                    .id(20L)
                    .booker(booker)
                    .item(item)
                    .status(Status.APPROVED)
                    .start(start)
                    .end(end)
                    .build();

            BookingDto result = realBookingMapper.toDto(source);

            assertNotNull(result);
            assertEquals(20L, result.getId());
            assertEquals(Status.APPROVED, result.getStatus());
            assertEquals(start, result.getStart());
            assertEquals(end, result.getEnd());

            assertNotNull(result.getBooker());
            assertEquals(2L, result.getBooker().getId());
            assertEquals("Booker", result.getBooker().getName());

            assertNotNull(result.getItem());
            assertEquals(10L, result.getItem().getId());
            assertEquals("Drill", result.getItem().getName());
        }

        @Test
        void toDtoNoRepoTest() {
            Booking source = Booking.builder()
                    .id(20L).booker(booker).item(item)
                    .status(Status.WAITING)
                    .start(LocalDateTime.now()).end(LocalDateTime.now().plusDays(1))
                    .build();

            realBookingMapper.toDto(source);

            verifyNoInteractions(userRepository, itemRepository);
        }

        @Test
        void toEntityNullTest() {
            assertNull(realBookingMapper.toEntity(null));
        }

        @Test
        void toEntityAllFieldsTest() {
            LocalDateTime start = LocalDateTime.of(2024, 6, 1, 9, 0);
            LocalDateTime end = LocalDateTime.of(2024, 6, 3, 18, 0);
            BookingDto dto = BookingDto.builder()
                    .id(20L)
                    .booker(BookingDto.Booker.builder().id(2L).name("Booker").build())
                    .item(BookingDto.Item.builder().id(10L).name("Drill").build())
                    .status(Status.APPROVED)
                    .start(start)
                    .end(end)
                    .build();
            when(userRepository.getReferenceById(2L)).thenReturn(booker);
            when(itemRepository.getReferenceById(10L)).thenReturn(item);

            Booking result = realBookingMapper.toEntity(dto);

            assertNotNull(result);
            assertEquals(20L, result.getId());
            assertSame(booker, result.getBooker());
            assertSame(item, result.getItem());
            assertEquals(Status.APPROVED, result.getStatus());
            assertEquals(start, result.getStart());
            assertEquals(end, result.getEnd());
            verify(userRepository).getReferenceById(2L);
            verify(itemRepository).getReferenceById(10L);
        }

        @Test
        void toEntityCreateNullTest() {
            assertNull(realBookingMapper.toEntityCreate(null, 2L));
            verifyNoInteractions(userRepository, itemRepository);
        }

        @Test
        void toEntityCreateFieldsTest() {
            LocalDateTime start = LocalDateTime.now().plusDays(1);
            LocalDateTime end = LocalDateTime.now().plusDays(2);
            BookingCreateDto dto = BookingCreateDto.builder()
                    .itemId(10L).start(start).end(end).build();
            when(itemRepository.findById(10L)).thenReturn(java.util.Optional.of(item));
            when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(booker));

            Booking result = realBookingMapper.toEntityCreate(dto, 2L);

            assertNotNull(result);
            assertNull(result.getId());
            assertSame(item, result.getItem());
            assertSame(booker, result.getBooker());
            assertEquals(start, result.getStart());
            assertEquals(end, result.getEnd());
            assertNull(result.getStatus());
        }

        @Test
        void toEntityCreateItemNotFoundTest() {
            BookingCreateDto dto = BookingCreateDto.builder()
                    .itemId(10L)
                    .start(LocalDateTime.now().plusDays(1))
                    .end(LocalDateTime.now().plusDays(2))
                    .build();
            when(itemRepository.findById(10L)).thenReturn(java.util.Optional.empty());

            assertThrows(ValidationNotObjectException.class,
                    () -> realBookingMapper.toEntityCreate(dto, 2L));
            verify(userRepository, never()).findById(any());
        }

        @Test
        void toEntityCreateBookerNotFoundTest() {
            BookingCreateDto dto = BookingCreateDto.builder()
                    .itemId(10L)
                    .start(LocalDateTime.now().plusDays(1))
                    .end(LocalDateTime.now().plusDays(2))
                    .build();
            when(itemRepository.findById(10L)).thenReturn(java.util.Optional.of(item));
            when(userRepository.findById(2L)).thenReturn(java.util.Optional.empty());

            assertThrows(ValidationNotObjectException.class,
                    () -> realBookingMapper.toEntityCreate(dto, 2L));
        }
    }
}