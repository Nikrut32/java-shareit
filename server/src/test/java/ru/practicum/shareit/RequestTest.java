package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dal.mappers.ItemMapper;
import ru.practicum.shareit.item.dto.RequestItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.request.dal.mappers.RequestMapper;
import ru.practicum.shareit.request.dto.GetItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.request.service.ItemRequestServiceImpl;
import ru.practicum.shareit.user.dal.mappers.UserMapper;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestTest {

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @Mock
    private RequestMapper requestMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRequestService itemRequestServiceMock;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemRequestServiceImpl itemRequestServiceImpl;

    private ItemRequestController itemRequestController;

    private User owner;
    private ItemRequest itemRequest;
    private ItemRequestDto itemRequestDto;
    private GetItemRequestDto getItemRequestDto;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .name("Owner")
                .email("owner@example.com")
                .build();

        itemRequest = ItemRequest.builder()
                .id(10L)
                .description("Need a drill")
                .created(LocalDateTime.now())
                .owner(owner)
                .build();

        itemRequestDto = ItemRequestDto.builder()
                .id(10L)
                .description("Need a drill")
                .created(itemRequest.getCreated())
                .build();

        getItemRequestDto = GetItemRequestDto.builder()
                .id(10L)
                .description("Need a drill")
                .created(itemRequest.getCreated())
                .build();

        itemRequestController = new ItemRequestController(itemRequestServiceMock);
    }

    @Test
    void createRequestTest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.save(itemRequest)).thenReturn(itemRequest);
        when(requestMapper.toDto(itemRequest)).thenReturn(itemRequestDto);

        assertEquals(itemRequestDto,
                itemRequestServiceImpl.createRequest(itemRequest, 1L));

        assertEquals(owner, itemRequest.getOwner());

        verify(itemRequestRepository).save(itemRequest);
        verify(requestMapper).toDto(itemRequest);
        verify(userRepository).findById(1L);
    }

    @Test
    void createRequestWithWrongUserTest() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> itemRequestServiceImpl.createRequest(itemRequest, 1L));

        verify(itemRequestRepository, never()).save(any());
        verify(requestMapper, never()).toDto(any());
    }

    @Test
    void createRequestSetOwnerTest() {
        User anotherOwner = User.builder()
                .id(2L)
                .name("Another")
                .email("another@example.com")
                .build();

        ItemRequest newRequest = ItemRequest.builder()
                .id(20L)
                .description("Need a hammer")
                .created(LocalDateTime.now())
                .build();

        ItemRequestDto expectedDto = ItemRequestDto.builder()
                .id(20L)
                .description("Need a hammer")
                .created(newRequest.getCreated())
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherOwner));
        when(itemRequestRepository.save(any(ItemRequest.class))).thenReturn(newRequest);
        when(requestMapper.toDto(newRequest)).thenReturn(expectedDto);

        itemRequestServiceImpl.createRequest(newRequest, 2L);

        ArgumentCaptor<ItemRequest> captor = ArgumentCaptor.forClass(ItemRequest.class);
        verify(itemRequestRepository).save(captor.capture());
        assertSame(anotherOwner, captor.getValue().getOwner());
    }

    @Test
    void getRequestsOwnerIdTest() {
        ItemRequest older = ItemRequest.builder()
                .id(1L)
                .description("Older request")
                .created(LocalDateTime.of(2026, 1, 1, 10, 0))
                .owner(owner)
                .build();

        ItemRequest newer = ItemRequest.builder()
                .id(2L)
                .description("Newer request")
                .created(LocalDateTime.of(2026, 1, 2, 10, 0))
                .owner(owner)
                .build();

        GetItemRequestDto olderDto = GetItemRequestDto.builder()
                .id(1L)
                .created(older.getCreated())
                .build();

        GetItemRequestDto newerDto = GetItemRequestDto.builder()
                .id(2L)
                .created(newer.getCreated())
                .build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRequestRepository.findByOwnerId(1L))
                .thenReturn(List.of(older, newer));
        when(requestMapper.toGetRequestDto(older)).thenReturn(olderDto);
        when(requestMapper.toGetRequestDto(newer)).thenReturn(newerDto);

        List<GetItemRequestDto> result =
                itemRequestServiceImpl.getRequestsOwnerId(1L);

        assertEquals(2, result.size());
        assertEquals(newerDto, result.get(0));
        assertEquals(olderDto, result.get(1));

        verify(itemRequestRepository).findByOwnerId(1L);
        verify(requestMapper, times(2)).toGetRequestDto(any());
    }

    @Test
    void getRequestsOwnerIdUserNotFoundTest() {
        when(userRepository.existsById(99L)).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> itemRequestServiceImpl.getRequestsOwnerId(99L));

        assertEquals("Ваш пользователь не зарегистрирован", ex.getMessage());
        verify(itemRequestRepository, never()).findByOwnerId(anyLong());
        verify(requestMapper, never()).toGetRequestDto(any());
    }

    @Test
    void getRequestsOwnerIdEmptyListTest() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRequestRepository.findByOwnerId(1L)).thenReturn(List.of());

        List<GetItemRequestDto> result =
                itemRequestServiceImpl.getRequestsOwnerId(1L);

        assertTrue(result.isEmpty());
        verify(requestMapper, never()).toGetRequestDto(any());
    }

    @Test
    void getRequestsOwnerIdSortByCreatedDescTest() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 1, 0, 0);

        ItemRequest r1 = ItemRequest.builder().id(1L).created(base.plusHours(1)).owner(owner).build();
        ItemRequest r2 = ItemRequest.builder().id(2L).created(base.plusHours(3)).owner(owner).build();
        ItemRequest r3 = ItemRequest.builder().id(3L).created(base.plusHours(2)).owner(owner).build();

        GetItemRequestDto d1 = GetItemRequestDto.builder().id(1L).created(r1.getCreated()).build();
        GetItemRequestDto d2 = GetItemRequestDto.builder().id(2L).created(r2.getCreated()).build();
        GetItemRequestDto d3 = GetItemRequestDto.builder().id(3L).created(r3.getCreated()).build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRequestRepository.findByOwnerId(1L)).thenReturn(List.of(r1, r3, r2));
        when(requestMapper.toGetRequestDto(r1)).thenReturn(d1);
        when(requestMapper.toGetRequestDto(r2)).thenReturn(d2);
        when(requestMapper.toGetRequestDto(r3)).thenReturn(d3);

        List<GetItemRequestDto> result =
                itemRequestServiceImpl.getRequestsOwnerId(1L);

        assertEquals(List.of(d2, d3, d1), result);
    }

    @Test
    void getRequestsAllTest() {
        ItemRequest older = ItemRequest.builder()
                .id(1L)
                .description("Older request")
                .created(LocalDateTime.of(2026, 1, 1, 10, 0))
                .owner(owner)
                .build();

        ItemRequest newer = ItemRequest.builder()
                .id(2L)
                .description("Newer request")
                .created(LocalDateTime.of(2026, 1, 2, 10, 0))
                .owner(owner)
                .build();

        ItemRequestDto olderDto = ItemRequestDto.builder()
                .id(1L)
                .created(older.getCreated())
                .build();

        ItemRequestDto newerDto = ItemRequestDto.builder()
                .id(2L)
                .created(newer.getCreated())
                .build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRequestRepository.findAll()).thenReturn(List.of(older, newer));
        when(requestMapper.toDto(older)).thenReturn(olderDto);
        when(requestMapper.toDto(newer)).thenReturn(newerDto);

        List<ItemRequestDto> result =
                itemRequestServiceImpl.getRequestsAll(1L);

        assertEquals(2, result.size());
        assertEquals(newerDto, result.get(0));
        assertEquals(olderDto, result.get(1));

        verify(itemRequestRepository).findAll();
        verify(requestMapper, times(2)).toDto(any());
    }

    @Test
    void getRequestsAllUserNotFoundTest() {
        when(userRepository.existsById(99L)).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> itemRequestServiceImpl.getRequestsAll(99L));

        assertEquals("Ваш пользователь не зарегистрирован", ex.getMessage());
        verify(itemRequestRepository, never()).findAll();
        verify(requestMapper, never()).toDto(any());
    }

    @Test
    void getRequestsAllEmptyListTest() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRequestRepository.findAll()).thenReturn(List.of());

        List<ItemRequestDto> result =
                itemRequestServiceImpl.getRequestsAll(1L);

        assertTrue(result.isEmpty());
        verify(requestMapper, never()).toDto(any());
    }

    @Test
    void getRequestByIdTest() {
        when(itemRequestRepository.findById(10L))
                .thenReturn(Optional.of(itemRequest));
        when(requestMapper.toGetRequestDto(itemRequest))
                .thenReturn(getItemRequestDto);

        assertEquals(getItemRequestDto,
                itemRequestServiceImpl.getRequestById(10L));

        verify(itemRequestRepository).findById(10L);
        verify(requestMapper).toGetRequestDto(itemRequest);
    }

    @Test
    void getRequestByIdNotFoundTest() {
        when(itemRequestRepository.findById(99L))
                .thenReturn(Optional.empty());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> itemRequestServiceImpl.getRequestById(99L));

        assertEquals("Запрос не найден", ex.getMessage());
        verify(requestMapper, never()).toGetRequestDto(any());
    }

    @Nested
    class ControllerTests {

        @Test
        void createItemRequestDelegateTest() {
            when(itemRequestServiceMock.createRequest(itemRequest, 1L))
                    .thenReturn(itemRequestDto);

            ItemRequestDto result =
                    itemRequestController.createItemRequest(itemRequest, 1L);

            assertEquals(itemRequestDto, result);
            verify(itemRequestServiceMock).createRequest(itemRequest, 1L);
        }

        @Test
        void getRequestsOwnerIdDelegateTest() {
            when(itemRequestServiceMock.getRequestsOwnerId(1L))
                    .thenReturn(List.of(getItemRequestDto));

            List<GetItemRequestDto> result =
                    itemRequestController.getRequestsOwnerId(1L);

            assertEquals(1, result.size());
            verify(itemRequestServiceMock).getRequestsOwnerId(1L);
        }

        @Test
        void getRequestsDelegateTest() {
            when(itemRequestServiceMock.getRequestsAll(1L))
                    .thenReturn(List.of(itemRequestDto));

            List<ItemRequestDto> result =
                    itemRequestController.getRequests(1L);

            assertEquals(1, result.size());
            verify(itemRequestServiceMock).getRequestsAll(1L);
        }

        @Test
        void getRequestByIdDelegateTest() {
            when(itemRequestServiceMock.getRequestById(10L))
                    .thenReturn(getItemRequestDto);

            GetItemRequestDto result =
                    itemRequestController.getRequestById(10L);

            assertEquals(getItemRequestDto, result);
            verify(itemRequestServiceMock).getRequestById(10L);
        }
    }

    @Nested
    class MapperTests {

        private RequestMapper realRequestMapper;

        @BeforeEach
        void setUpRequestMapperTest() {
            realRequestMapper = new RequestMapper(userMapper, itemRepository, itemMapper);
        }

        @Test
        void toDtoAllFieldsTest() {
            UserDto ownerDto = UserDto.builder().id(1L).name("Owner").build();
            when(userMapper.toDto(owner)).thenReturn(ownerDto);

            ItemRequestDto result = realRequestMapper.toDto(itemRequest);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals("Need a drill", result.getDescription());
            assertEquals(itemRequest.getCreated(), result.getCreated());
            assertEquals(ownerDto, result.getOwner());
            verify(userMapper).toDto(owner);
            verifyNoInteractions(itemRepository, itemMapper);
        }

        @Test
        void toDtoDiffOwnerDelegatesTest() {
            User anotherOwner = User.builder().id(7L).name("Another").build();
            ItemRequest source = ItemRequest.builder()
                    .id(11L).description("Need a saw").created(LocalDateTime.now())
                    .owner(anotherOwner).build();
            UserDto ownerDto = UserDto.builder().id(7L).name("Another").build();
            when(userMapper.toDto(anotherOwner)).thenReturn(ownerDto);

            ItemRequestDto result = realRequestMapper.toDto(source);

            assertEquals(11L, result.getId());
            assertEquals("Need a saw", result.getDescription());
            assertEquals(ownerDto, result.getOwner());
        }

        @Test
        void toEntityNullTest() {
            assertNull(realRequestMapper.toEntity(null));
            verifyNoInteractions(userMapper, itemRepository, itemMapper);
        }

        @Test
        void toEntityAllFieldsTest() {
            UserDto ownerDto = UserDto.builder().id(1L).name("Owner").build();
            ItemRequestDto dto = ItemRequestDto.builder()
                    .id(10L)
                    .description("Need a drill")
                    .created(LocalDateTime.of(2026, 1, 1, 10, 0))
                    .owner(ownerDto)
                    .build();
            when(userMapper.toEntity(ownerDto)).thenReturn(owner);

            ItemRequest result = realRequestMapper.toEntity(dto);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals("Need a drill", result.getDescription());
            assertEquals(dto.getCreated(), result.getCreated());
            assertSame(owner, result.getOwner());
            verify(userMapper).toEntity(ownerDto);
        }

        @Test
        void toGetRequestDtoFieldsEmptyItemsTest() {
            when(itemRepository.findByRequestId(10L)).thenReturn(List.of());

            GetItemRequestDto result = realRequestMapper.toGetRequestDto(itemRequest);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals("Need a drill", result.getDescription());
            assertEquals(itemRequest.getCreated(), result.getCreated());
            assertNotNull(result.getItems());
            assertTrue(result.getItems().isEmpty());
            verify(itemRepository).findByRequestId(10L);
            verify(itemMapper, never()).toRequestDto(any());
        }

        @Test
        void toGetRequestDtoItemsTest() {
            Item drill = Item.builder().id(100L).name("Drill").owner(owner).build();
            Item saw = Item.builder().id(101L).name("Saw").owner(owner).build();
            RequestItemDto drillDto = RequestItemDto.builder().id(100L).name("Drill").ownerId(1L).build();
            RequestItemDto sawDto = RequestItemDto.builder().id(101L).name("Saw").ownerId(1L).build();

            when(itemRepository.findByRequestId(10L)).thenReturn(List.of(drill, saw));
            when(itemMapper.toRequestDto(drill)).thenReturn(drillDto);
            when(itemMapper.toRequestDto(saw)).thenReturn(sawDto);

            GetItemRequestDto result = realRequestMapper.toGetRequestDto(itemRequest);

            assertEquals(2, result.getItems().size());
            assertEquals(drillDto, result.getItems().get(0));
            assertEquals(sawDto, result.getItems().get(1));
            verify(itemMapper).toRequestDto(drill);
            verify(itemMapper).toRequestDto(saw);
        }

        @Test
        void toGetRequestDtoNoUserMapperTest() {
            when(itemRepository.findByRequestId(10L)).thenReturn(List.of());

            realRequestMapper.toGetRequestDto(itemRequest);

            verifyNoInteractions(userMapper);
        }
    }
}