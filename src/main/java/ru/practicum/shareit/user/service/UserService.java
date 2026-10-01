package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

public interface UserService {
    UserDto createUser(User user);

    UserDto updateUser(long id, UpdateUserRequest updateUser);
}
