package ru.practicum.shareit.item.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.AccessException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.item.dal.mappers.ItemMapper;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemTimeDto;
import ru.practicum.shareit.item.dto.UpdateItemRequest;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.dto.ItemResponse;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemMapper itemMapper;

    @Transactional
    @Override
    public List<ItemTimeDto> getAllItems(long ownerId) {
        return itemRepository.findByOwnerId(ownerId).stream()
                .map(itemMapper::mapToTimeDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public List<ItemDto> getItemSearchText(String searchText) {
        if  (searchText == null || searchText.isEmpty()) {
            return List.of();
        }
        return itemRepository.findAll().stream()
                .filter(item -> (item.getDescription().toLowerCase().contains(searchText.toLowerCase())
                        || item.getName().toLowerCase().contains(searchText.toLowerCase())) && item.getAvailable())
                .map(itemMapper::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ItemResponse getItemById(long itemId, long ownerId) {
        if (!userRepository.existsById(ownerId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ValidationNotObjectException("Предмет с id: " + itemId + " не найден"));
        if (ownerId == item.getOwner().getId()) {
            return itemMapper.mapToCreateDto(item);
        } else  {
            return itemMapper.mapToTimeDto(item);
        }
    }

    @Transactional
    @Override
    public Item createItem(ItemDto itemDto, Long ownerId) {
        exceptionItem(itemDto);
        if (!userRepository.existsById(ownerId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }

        Item item = itemMapper.toEntity(itemDto, ownerId, null);

        return itemRepository.save(item);
    }

    @Transactional
    @Override
    public ItemDto updateItem(UpdateItemRequest updateItem, long itemId, Long ownerId) {
        if (!userRepository.existsById(ownerId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }
        if (!itemRepository.existsByIdAndOwnerId(itemId, ownerId)) {
            throw new AccessException("Редактировать запись, может только владелец");
        }
        Item item = itemRepository.getReferenceById(itemId);
        if (updateItem.hasName()) {
            item.setName(updateItem.getName());
        }
        if (updateItem.hasDescription()) {
            item.setDescription(updateItem.getDescription());
        }
        if (updateItem.hasAvailable()) {
            item.setAvailable(updateItem.getAvailable());
        }
        return itemMapper.mapToDto(itemRepository.save(item));
    }

    @Transactional
    @Override
    public void deleteItem(long itemId, Long ownerId) {
        if (!userRepository.existsById(ownerId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }
        if (itemRepository.existsByIdAndOwnerId(itemId, ownerId)) {
            throw new AccessException("Удалять запись может только владелец");
        }
        itemRepository.deleteById(itemId);
    }

    @Transactional
    @Override
    public void increaseRental(long itemId) {
        Item item = itemRepository.getReferenceById(itemId);
        Long currentCount = item.getCountRental();
        item.setCountRental(currentCount == null ? 1L : currentCount + 1);
        itemRepository.save(item);
    }

    @Transactional
    private void exceptionItem(ItemDto item) {
        if (item.getName() == null) {
            log.warn("Валидация не пройдена: название не было указан");
            throw new ValidationException("Название не было указан");
        }
        if (item.getName().isBlank()) {
            log.warn("Валидация не пройдена: название пустое");
            throw new ValidationException("Название не может быть пустым");
        }
        if (item.getDescription() == null) {
            log.warn("Валидация не пройдена: email не был указан");
            throw new ValidationException("Email не был указан");
        }
        if (item.getDescription().isBlank()) {
            log.warn("Валидация не пройдена: email пустой");
            throw new ValidationException("Указан пустой email");
        }
        if (item.getAvailable() == null) {
            log.warn("Валидация не пройдена: статус доступа не был указан");
            throw new ValidationException("статус доступа не был указан");
        }
    }
}
