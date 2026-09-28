package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.AccessException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.UpdateItemRequest;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.model.ItemResponse;
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

    @Override
    public List<ItemDto> getAllItems(long ownerId) {
        return itemRepository.getAllItems(ownerId).stream()
                .map(item -> ItemDto.builder().id(item.getId())
                        .name(item.getName())
                        .description(item.getDescription())
                        .available(item.getAvailable())
                        .countRental(item.getCountRental())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> getItemSearchText(String searchText) {
        if  (searchText == null || searchText.isEmpty()) {
            return List.of();
        }

        return itemRepository.getItemSearchText(searchText).stream()
                .map(item -> ItemDto.builder().id(item.getId())
                        .name(item.getName())
                        .description(item.getDescription())
                        .available(item.getAvailable())
                        .countRental(item.getCountRental())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public ItemResponse getItemById(long itemId, long ownerId) {
        if (!userRepository.checkUserById(ownerId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }

        Item item = itemRepository.getItemById(itemId);
        if (ownerId == item.getOwnerId()) {
            return item;
        } else  {
            return ItemDto.builder()
                    .id(item.getId())
                    .name(item.getName())
                    .description(item.getDescription())
                    .countRental(item.getCountRental())
                    .build();
        }
    }

    @Override
    public Item createItem(ItemDto itemDto, Long ownerId) {
        exceptionItem(itemDto);
        if (!userRepository.checkUserById(ownerId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }

        return itemRepository.createItem(itemDto, ownerId, null);
    }

    @Override
    public ItemDto updateItem(UpdateItemRequest updateItem, long itemId, Long ownerId) {
        if (!userRepository.checkUserById(ownerId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }
        if (!itemRepository.checkOwner(itemId, ownerId)) {
            throw new AccessException("Редактировать запись, может только владелец");
        }
        Item item = itemRepository.getItemById(itemId);
        ItemDto itemDto = ItemDto.builder()
                .id(itemId)
                .name(item.getName())
                .description(item.getDescription())
                .countRental(item.getCountRental())
                .available(item.getAvailable())
                .build();
        if (updateItem.hasName()) {
            itemDto.setName(updateItem.getName());
        }
        if (updateItem.hasDescription()) {
            itemDto.setDescription(updateItem.getDescription());
        }
        if (updateItem.hasAvailable()) {
            itemDto.setAvailable(updateItem.getAvailable());
        }
        return itemRepository.updateItem(itemDto);
    }

    @Override
    public void deleteItem(long itemId, Long ownerId) {
        if (!userRepository.checkUserById(ownerId)) {
            throw new ValidationNotObjectException("Ваш пользователь не зарегистрирован");
        }
        if (!itemRepository.checkOwner(itemId, ownerId)) {
            throw new AccessException("Удалять запись может только владелец");
        }
        itemRepository.deleteItem(itemId);
    }

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
