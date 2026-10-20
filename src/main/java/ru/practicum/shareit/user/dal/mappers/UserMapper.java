package ru.practicum.shareit.user.dal.mappers;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

@Component
public class UserMapper {
    public UserDto toDto(User user) {
        if  (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public User toEntity(UserDto dto) {
        if  (dto == null) {
            return null;
        }

        return User.builder()
                .id(dto.getId())
                .name(dto.getName())
                .email(dto.getEmail())
                .build();
    }
}
