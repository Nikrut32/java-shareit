package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.Status;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.AccessException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.item.dal.mappers.CommentMapper;
import ru.practicum.shareit.item.dal.mappers.ItemMapper;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.CommentServiceImpl;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.item.service.ItemServiceImpl;
import ru.practicum.shareit.item.service.CommentService;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.base.BaseStringResponse;
import ru.practicum.shareit.item.ItemController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ItemRequestRepository itemRequestRepository;

    @InjectMocks
    private CommentServiceImpl commentService;
    @InjectMocks
    private ItemServiceImpl itemService;

    private Item item;
    private User booker;
    private CommentCreateDto request;
    private Comment comment;

    @BeforeEach
    void setUp() {
        booker = User.builder().id(2L).name("Booker").email("booker@example.com").build();
        item = Item.builder().id(10L).name("Drill").description("Electric drill").available(true).countRental(0L).comments(List.of()).build();
        request = CommentCreateDto.builder().text("Works well").build();
        comment = Comment.builder().id(5L).author(booker).item(item).text("Works well").build();
    }

    @Test
    void createItemExistingOwnerTest() {
        ItemDto requestDto = ItemDto.builder().name("Drill").description("Electric drill").available(true).build();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemMapper.toEntity(requestDto, 1L, null)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);

        assertEquals(item, itemService.createItem(requestDto, 1L));
        verify(itemRepository).save(item);
    }

    @Test
    void createItemUnknownOwnerTest() {
        ItemDto requestDto = ItemDto.builder().name("Drill").description("Description").available(true).build();
        when(userRepository.existsById(1L)).thenReturn(false);
        assertThrows(ValidationNotObjectException.class, () -> itemService.createItem(requestDto, 1L));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void searchAvailableMatchingIgnoreCaseTest() {
        Item unavailable = Item.builder().id(11L).name("Drill").description("Tool").available(false).comments(List.of()).build();
        when(itemRepository.findAll()).thenReturn(List.of(item, unavailable));
        when(itemMapper.mapToDto(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            return ItemDto.builder().id(i.getId()).name(i.getName()).build();
        });

        List<ItemDto> result = itemService.getItemSearchText("DRILL");

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void searchByDescriptionIgnoreCaseTest() {
        when(itemRepository.findAll()).thenReturn(List.of(item));
        when(itemMapper.mapToDto(any(Item.class))).thenReturn(ItemDto.builder().id(10L).build());

        List<ItemDto> result = itemService.getItemSearchText("ELECTRIC");

        assertEquals(1, result.size());
        verify(itemMapper).mapToDto(any(Item.class));
    }

    @Test
    void searchEmptyWhenNoMatchTest() {
        when(itemRepository.findAll()).thenReturn(List.of(item));

        assertTrue(itemService.getItemSearchText("hammer").isEmpty());
        verify(itemMapper, never()).mapToDto(any());
    }

    @Test
    void searchBlankTextReturnsEmptyTest() {
        assertTrue(itemService.getItemSearchText("").isEmpty());
        verify(itemRepository, never()).findAll();
    }

    @Test
    void searchNullTextReturnsEmptyTest() {
        assertTrue(itemService.getItemSearchText(null).isEmpty());
        verify(itemRepository, never()).findAll();
    }

    @Test
    void updateItemFieldsForOwnerTest() {
        UpdateItemRequest request = new UpdateItemRequest();
        request.setName("New drill");

        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.existsByIdAndOwnerId(10L, 1L)).thenReturn(true);
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itemMapper.mapToDto(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            return ItemDto.builder().id(i.getId()).name(i.getName()).description(i.getDescription()).available(i.getAvailable()).build();
        });

        ItemDto result = itemService.updateItem(request, 10L, 1L);

        assertNotNull(result);
        assertEquals("New drill", result.getName());
        assertEquals("Electric drill", result.getDescription());
        verify(itemMapper).mapToDto(any(Item.class));
    }

    @Test
    void updateItemDescAndAvailableTest() {
        UpdateItemRequest request = new UpdateItemRequest();
        request.setDescription("New description");
        request.setAvailable(false);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.existsByIdAndOwnerId(10L, 1L)).thenReturn(true);
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itemMapper.mapToDto(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            return ItemDto.builder().id(i.getId()).name(i.getName()).description(i.getDescription()).available(i.getAvailable()).build();
        });

        ItemDto result = itemService.updateItem(request, 10L, 1L);

        assertNotNull(result);
        assertEquals("New description", result.getDescription());
        assertFalse(result.getAvailable());
        assertEquals("Drill", result.getName());
    }

    @Test
    void updateItemEmptyRequestNoChangeTest() {
        UpdateItemRequest request = new UpdateItemRequest();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.existsByIdAndOwnerId(10L, 1L)).thenReturn(true);
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itemMapper.mapToDto(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            return ItemDto.builder().id(i.getId()).name(i.getName()).description(i.getDescription()).available(i.getAvailable()).build();
        });

        ItemDto result = itemService.updateItem(request, 10L, 1L);

        assertNotNull(result);
        assertEquals("Drill", result.getName());
        assertEquals("Electric drill", result.getDescription());
        assertTrue(result.getAvailable());
    }

    @Test
    void updateItemNonOwnerTest() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.existsByIdAndOwnerId(10L, 1L)).thenReturn(false);

        assertThrows(AccessException.class, () -> itemService.updateItem(new UpdateItemRequest(), 10L, 1L));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItemUnknownUserTest() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ValidationNotObjectException.class, () -> itemService.updateItem(new UpdateItemRequest(), 10L, 1L));
        verify(itemRepository, never()).existsByIdAndOwnerId(anyLong(), anyLong());
    }

    @Test
    void deleteItemForOwnerTest() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.existsByIdAndOwnerId(10L, 1L)).thenReturn(true);

        itemService.deleteItem(10L, 1L);

        verify(itemRepository).deleteById(10L);
    }

    @Test
    void deleteItemUnknownUserTest() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ValidationNotObjectException.class, () -> itemService.deleteItem(10L, 1L));
        verify(itemRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteItemNonOwnerTest() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.existsByIdAndOwnerId(10L, 1L)).thenReturn(false);

        assertThrows(AccessException.class, () -> itemService.deleteItem(10L, 1L));
        verify(itemRepository, never()).deleteById(anyLong());
    }

    @Test
    void increaseRentalIncrementsCountTest() {
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);

        itemService.increaseRental(10L);

        assertEquals(1L, item.getCountRental());
        verify(itemRepository).save(item);
    }

    @Test
    void increaseRentalInitWhenNullTest() {
        Item nullCountItem = Item.builder().id(10L).countRental(null).build();
        when(itemRepository.getReferenceById(10L)).thenReturn(nullCountItem);
        when(itemRepository.save(nullCountItem)).thenReturn(nullCountItem);

        itemService.increaseRental(10L);

        assertEquals(1L, nullCountItem.getCountRental());
    }

    @Test
    void getAllItemsMappedForOwnerTest() {
        ItemTimeDto dto = ItemTimeDto.builder().id(10L).name("Drill").build();
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of(item));
        when(itemMapper.mapToTimeDto(any(Item.class))).thenReturn(dto);

        List<ItemTimeDto> result = itemService.getAllItems(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        verify(itemRepository).findByOwnerId(1L);
        verify(itemMapper).mapToTimeDto(any(Item.class));
    }

    @Test
    void getAllItemsEmptyListTest() {
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of());

        assertTrue(itemService.getAllItems(1L).isEmpty());
        verify(itemMapper, never()).mapToTimeDto(any());
    }

    @Test
    void getItemByIdUnknownUserTest() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ValidationNotObjectException.class, () -> itemService.getItemById(10L, 1L));
        verify(itemRepository, never()).findById(anyLong());
    }

    @Test
    void getItemByIdUnknownItemTest() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ValidationNotObjectException.class, () -> itemService.getItemById(10L, 1L));
    }

    @Test
    void createCommentAfterCompletedApprovedBookingTest() {
        Booking completed = Booking.builder().item(item).booker(booker).status(Status.APPROVED).end(LocalDateTime.now().minusDays(1)).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentMapper.toEntityCreate(request, booker, item)).thenReturn(comment);
        when(bookingRepository.findAll()).thenReturn(List.of(completed));
        when(commentRepository.save(comment)).thenReturn(comment);
        CommentDto dto = CommentDto.builder().id(5L).text("Works well").build();
        when(commentMapper.toDto(any(Comment.class))).thenReturn(dto);

        assertEquals(dto, commentService.createComment(request, 2L, 10L));
        verify(commentRepository).save(comment);
    }

    @Test
    void createCommentNoCompletedBookingTest() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentMapper.toEntityCreate(request, booker, item)).thenReturn(comment);
        when(bookingRepository.findAll()).thenReturn(List.of());

        assertThrows(ValidationException.class, () -> commentService.createComment(request, 2L, 10L));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void createCommentUnknownUserTest() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ValidationNotObjectException.class, () -> commentService.createComment(request, 2L, 10L));
    }

    @Test
    void createCommentUnknownItemTest() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ValidationNotObjectException.class, () -> commentService.createComment(request, 2L, 10L));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void createCommentBookingNotFinishedTest() {
        Booking future = Booking.builder().item(item).booker(booker).status(Status.APPROVED).end(LocalDateTime.now().plusDays(1)).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentMapper.toEntityCreate(request, booker, item)).thenReturn(comment);
        when(bookingRepository.findAll()).thenReturn(List.of(future));

        assertThrows(ValidationException.class, () -> commentService.createComment(request, 2L, 10L));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void createCommentWrongStatusTest() {
        Booking rejected = Booking.builder().item(item).booker(booker).status(Status.REJECTED).end(LocalDateTime.now().minusDays(1)).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentMapper.toEntityCreate(request, booker, item)).thenReturn(comment);
        when(bookingRepository.findAll()).thenReturn(List.of(rejected));

        assertThrows(ValidationException.class, () -> commentService.createComment(request, 2L, 10L));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void createCommentBookingWrongItemTest() {
        Item anotherItem = Item.builder().id(99L).name("Saw").description("Hand saw").available(true).countRental(0L).comments(List.of()).build();
        Booking otherItemBooking = Booking.builder().item(anotherItem).booker(booker).status(Status.APPROVED).end(LocalDateTime.now().minusDays(1)).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentMapper.toEntityCreate(request, booker, item)).thenReturn(comment);
        when(bookingRepository.findAll()).thenReturn(List.of(otherItemBooking));

        assertThrows(ValidationException.class, () -> commentService.createComment(request, 2L, 10L));
        verify(commentRepository, never()).save(any());
    }

    @Nested
    class ControllerTests {

        @Mock
        private ItemService itemServiceMock;
        @Mock
        private ItemMapper itemMapperMock;
        @Mock
        private CommentService commentServiceMock;

        private ItemController controller;

        @BeforeEach
        void setUpController() {
            controller = new ItemController(itemServiceMock, itemMapperMock, commentServiceMock);
        }

        @Test
        void getItemsDelegateTest() {
            ItemTimeDto dto = ItemTimeDto.builder().id(10L).name("Drill").build();
            when(itemServiceMock.getAllItems(1L)).thenReturn(List.of(dto));

            List<ItemTimeDto> result = controller.getItems(1L);

            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getId());
            verify(itemServiceMock).getAllItems(1L);
        }

        @Test
        void getItemsEmptyListTest() {
            when(itemServiceMock.getAllItems(1L)).thenReturn(List.of());

            assertTrue(controller.getItems(1L).isEmpty());
            verify(itemServiceMock).getAllItems(1L);
        }

        @Test
        void getItemDelegateTest() {
            ItemCreateDto createDto = ItemCreateDto.builder().id(10L).name("Drill").build();
            when(itemServiceMock.getItemById(10L, 1L)).thenReturn(createDto);

            ItemResponse result = controller.getItem(10L, 1L);

            assertEquals(createDto, result);
            verify(itemServiceMock).getItemById(10L, 1L);
        }

        @Test
        void getItemPathAndHeaderTest() {
            when(itemServiceMock.getItemById(anyLong(), anyLong())).thenReturn(ItemTimeDto.builder().id(5L).build());

            controller.getItem(5L, 7L);

            verify(itemServiceMock).getItemById(5L, 7L);
        }

        @Test
        void getItemSearchTextDelegateTest() {
            ItemDto dto = ItemDto.builder().id(10L).name("Drill").build();
            when(itemServiceMock.getItemSearchText("drill")).thenReturn(List.of(dto));

            List<ItemDto> result = controller.getItemSearchText("drill");

            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getId());
            verify(itemServiceMock).getItemSearchText("drill");
        }

        @Test
        void getItemSearchTextEmptyTest() {
            when(itemServiceMock.getItemSearchText("hammer")).thenReturn(List.of());

            assertTrue(controller.getItemSearchText("hammer").isEmpty());
        }

        @Test
        void createItemDelegateAndMapTest() {
            ItemDto request = ItemDto.builder().name("Drill").description("Electric drill").available(true).build();
            Item saved = Item.builder().id(10L).name("Drill").build();
            ItemCreateDto response = ItemCreateDto.builder().id(10L).name("Drill").build();

            when(itemServiceMock.createItem(request, 1L)).thenReturn(saved);
            when(itemMapperMock.mapToCreateDto(any(Item.class))).thenReturn(response);

            ItemCreateDto result = controller.createItem(1L, request);

            assertEquals(response, result);
            verify(itemServiceMock).createItem(request, 1L);
            verify(itemMapperMock).mapToCreateDto(any(Item.class));
        }

        @Test
        void createCommentDelegateTest() {
            CommentCreateDto request = CommentCreateDto.builder().text("Works well").build();
            CommentDto response = CommentDto.builder().id(5L).text("Works well").build();

            when(commentServiceMock.createComment(request, 2L, 10L)).thenReturn(response);

            CommentDto result = controller.createComment(2L, request, 10L);

            assertEquals(response, result);
            verify(commentServiceMock).createComment(request, 2L, 10L);
        }

        @Test
        void updateItemDelegateTest() {
            UpdateItemRequest request = new UpdateItemRequest();
            request.setName("New drill");
            ItemDto response = ItemDto.builder().id(10L).name("New drill").build();

            when(itemServiceMock.updateItem(request, 10L, 1L)).thenReturn(response);

            ItemDto result = controller.updateItem(10L, 1L, request);

            assertEquals(response, result);
            verify(itemServiceMock).updateItem(request, 10L, 1L);
        }

        @Test
        void deleteItemDelegateAndMessageTest() {
            BaseStringResponse result = controller.deleteItem(10L, 1L);

            assertNotNull(result);
            verify(itemServiceMock).deleteItem(10L, 1L);
        }

        @Test
        void deleteItemCallOnceTest() {
            controller.deleteItem(33L, 4L);

            verify(itemServiceMock, times(1)).deleteItem(33L, 4L);
        }
    }

    @Nested
    class MapperTests {

        private CommentMapper realCommentMapper;

        @BeforeEach
        void setUpCommentMapper() {
            realCommentMapper = new CommentMapper();
        }

        @Test
        void commentMapperToDtoAllFieldsTest() {
            User author = User.builder().id(2L).name("Booker").build();
            Item commentItem = Item.builder().id(10L).name("Drill").build();
            LocalDateTime created = LocalDateTime.of(2024, 1, 15, 10, 30);
            Comment source = Comment.builder().id(5L).text("Works well").author(author).item(commentItem).created(created).build();

            CommentDto result = realCommentMapper.toDto(source);

            assertNotNull(result);
            assertEquals(5L, result.getId());
            assertEquals("Works well", result.getText());
            assertEquals("Booker", result.getAuthorName());
            assertNotNull(result.getItem());
            assertEquals(10L, result.getItem().getId());
            assertEquals("Drill", result.getItem().getName());
            assertEquals(created, result.getCreated());
        }

        @Test
        void commentMapperToEntityCreateFieldsTest() {
            User author = User.builder().id(2L).name("Booker").build();
            Item commentItem = Item.builder().id(10L).name("Drill").build();
            CommentCreateDto dto = CommentCreateDto.builder().id(7L).text("Great tool").build();

            Comment result = realCommentMapper.toEntityCreate(dto, author, commentItem);

            assertNotNull(result);
            assertEquals(7L, result.getId());
            assertEquals("Great tool", result.getText());
            assertSame(author, result.getAuthor());
            assertSame(commentItem, result.getItem());
            assertNull(result.getCreated());
        }

        @Test
        void commentMapperToEntityCreateNullIdTest() {
            User author = User.builder().id(2L).name("Booker").build();
            Item commentItem = Item.builder().id(10L).name("Drill").build();
            CommentCreateDto dto = CommentCreateDto.builder().text("No id yet").build();

            Comment result = realCommentMapper.toEntityCreate(dto, author, commentItem);

            assertNull(result.getId());
            assertEquals("No id yet", result.getText());
            assertSame(author, result.getAuthor());
            assertSame(commentItem, result.getItem());
        }

        private ItemMapper realItemMapper;

        @BeforeEach
        void setUpItemMapper() {
            realItemMapper = new ItemMapper(userRepository, bookingRepository, commentMapper, itemRequestRepository);
            org.springframework.test.util.ReflectionTestUtils.setField(realItemMapper, "userMapper", new ru.practicum.shareit.user.dal.mappers.UserMapper());
        }

        @Test
        void itemMapperToDtoNullTest() {
            assertNull(realItemMapper.mapToDto(null));
        }

        @Test
        void itemMapperToDtoFieldsAndCommentsTest() {
            Item source = Item.builder().id(10L).name("Drill").description("Electric drill").available(true).countRental(3L).comments(List.of()).build();

            ItemDto result = realItemMapper.mapToDto(source);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals("Drill", result.getName());
            assertEquals("Electric drill", result.getDescription());
            assertEquals(true, result.getAvailable());
            assertEquals(3L, result.getCountRental());
            assertNull(result.getRequestId());
            assertTrue(result.getComments().isEmpty());
        }

        @Test
        void itemMapperToDtoCommentsTest() {
            Comment c = Comment.builder().id(5L).text("Works well").author(booker).item(item).build();
            Item source = Item.builder().id(10L).name("Drill").available(true).countRental(0L).comments(List.of(c)).build();
            CommentDto commentDto = CommentDto.builder().id(5L).text("Works well").build();
            when(commentMapper.toDto(c)).thenReturn(commentDto);

            ItemDto result = realItemMapper.mapToDto(source);

            assertEquals(1, result.getComments().size());
            assertEquals(5L, result.getComments().get(0).getId());
            verify(commentMapper).toDto(c);
        }

        @Test
        void itemMapperToCreateDtoNullTest() {
            assertNull(realItemMapper.mapToCreateDto(null));
        }

        @Test
        void itemMapperToCreateDtoFieldsTest() {
            User owner = User.builder().id(1L).name("Owner").email("o@e.com").build();
            Item source = Item.builder().id(10L).name("Drill").description("Electric drill").available(true).countRental(0L).owner(owner).comments(List.of()).build();

            ItemCreateDto result = realItemMapper.mapToCreateDto(source);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals("Drill", result.getName());
            assertNotNull(result.getOwner());
            assertEquals("Owner", result.getOwner().getName());
        }

        @Test
        void itemMapperToTimeDtoNullThrowsTest() {
            assertThrows(NullPointerException.class, () -> realItemMapper.mapToTimeDto(null));
        }

        @Test
        void itemMapperToTimeDtoNextAndLastTest() {
            Item source = Item.builder().id(10L).name("Drill").description("Electric drill").available(true).countRental(0L).comments(List.of()).build();
            LocalDateTime now = LocalDateTime.now();
            Booking past = Booking.builder().item(source).status(Status.APPROVED).start(now.minusDays(5)).end(now.minusDays(3)).build();
            Booking future = Booking.builder().item(source).status(Status.APPROVED).start(now.plusDays(3)).end(now.plusDays(5)).build();
            when(bookingRepository.findAll()).thenReturn(List.of(past, future));

            ItemTimeDto result = realItemMapper.mapToTimeDto(source);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals(now.plusDays(3).toLocalDate(), result.getNextBooking());
            assertEquals(now.minusDays(3).toLocalDate(), result.getLastBooking());
        }

        @Test
        void itemMapperToTimeDtoNoBookingsNullTest() {
            Item source = Item.builder().id(10L).name("Drill").available(true).countRental(0L).comments(List.of()).build();
            when(bookingRepository.findAll()).thenReturn(List.of());

            ItemTimeDto result = realItemMapper.mapToTimeDto(source);

            assertNull(result.getNextBooking());
            assertNull(result.getLastBooking());
        }

        @Test
        void itemMapperToEntityNullTest() {
            assertNull(realItemMapper.toEntity(null, 1L, null));
        }

        @Test
        void itemMapperToEntityFieldsTest() {
            ItemDto dto = ItemDto.builder().id(10L).name("Drill").description("Electric drill").available(true).countRental(0L).build();

            Item result = realItemMapper.toEntity(dto, null, null);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals("Drill", result.getName());
            assertNull(result.getOwner());
            assertNull(result.getRequest());
            verify(userRepository, never()).getReferenceById(anyLong());
            verify(itemRequestRepository, never()).getReferenceById(anyLong());
        }

        @Test
        void itemMapperToRequestDtoFieldsTest() {
            User owner = User.builder().id(1L).name("Owner").build();
            Item source = Item.builder().id(10L).name("Drill").owner(owner).build();

            RequestItemDto result = realItemMapper.toRequestDto(source);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals("Drill", result.getName());
            assertEquals(1L, result.getOwnerId());
        }
    }
}