package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.AccessException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.item.dal.mappers.ItemMapper;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.UpdateItemRequest;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemServiceImpl;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {
    @Mock private ItemRepository itemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemMapper itemMapper;
    @InjectMocks private ItemServiceImpl itemService;

    private Item item;

    @BeforeEach
    void setUp() {
        item = Item.builder().id(10L).name("Drill").description("Electric drill")
                .available(true).countRental(0L).comments(List.of()).build();
    }

    @Test
    void createItemSavesItemForExistingOwner() {
        ItemDto request = ItemDto.builder().name("Drill").description("Electric drill")
                .available(true).build();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemMapper.toEntity(request, 1L, null)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);

        assertEquals(item, itemService.createItem(request, 1L));
        verify(itemRepository).save(item);
    }

    @Test
    void createItemRejectsBlankName() {
        ItemDto request = ItemDto.builder().name(" ").description("Description").available(true).build();
        assertThrows(ValidationException.class, () -> itemService.createItem(request, 1L));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void createItemRejectsUnknownOwner() {
        ItemDto request = ItemDto.builder().name("Drill").description("Description").available(true).build();
        when(userRepository.existsById(1L)).thenReturn(false);
        assertThrows(ValidationNotObjectException.class, () -> itemService.createItem(request, 1L));
    }

    @Test
    void searchReturnsOnlyAvailableMatchingItemsIgnoringCase() {
        Item unavailable = Item.builder().id(11L).name("Drill").description("Tool")
                .available(false).comments(List.of()).build();
        when(itemRepository.findAll()).thenReturn(List.of(item, unavailable));
        when(itemMapper.mapToDto(item)).thenReturn(ItemDto.builder().id(10L).name("Drill").build());

        List<ItemDto> result = itemService.getItemSearchText("DRILL");

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void searchWithBlankTextReturnsEmptyList() {
        assertTrue(itemService.getItemSearchText("").isEmpty());
        verify(itemRepository, never()).findAll();
    }

    @Test
    void updateItemChangesProvidedFieldsForOwner() {
        UpdateItemRequest request = new UpdateItemRequest();
        request.setName("New drill");
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.existsByIdAndOwnerId(10L, 1L)).thenReturn(true);
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.mapToDto(item)).thenAnswer(invocation -> ItemDto.builder()
                .id(item.getId()).name(item.getName()).description(item.getDescription())
                .available(item.getAvailable()).build());

        ItemDto result = itemService.updateItem(request, 10L, 1L);

        assertEquals("New drill", result.getName());
        assertEquals("Electric drill", result.getDescription());
    }

    @Test
    void updateItemRejectsNonOwner() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.existsByIdAndOwnerId(10L, 1L)).thenReturn(false);
        assertThrows(AccessException.class,
                () -> itemService.updateItem(new UpdateItemRequest(), 10L, 1L));
    }

    @Test
    void increaseRentalIncrementsCount() {
        when(itemRepository.getReferenceById(10L)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);
        itemService.increaseRental(10L);
        assertEquals(1L, item.getCountRental());
        verify(itemRepository).save(item);
    }
}
