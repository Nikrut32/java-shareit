package ru.practicum.shareit.item.dal.mappers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.repository.UserRepository;

@Component
public class ItemMapper {
    @Autowired
    private UserRepository userRepository;

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
                .build();
    }

    public Item toEntity(ItemDto dto, long ownerId, long requestId) {
        if  (dto == null) {
            return null;
        }

        return Item.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .available(dto.getAvailable())
                .countRental(dto.getCountRental())
                .owner(userRepository.getUserById(ownerId))
                .request(ItemRequest.builder().id(requestId).build())
                .build();
    }
}
