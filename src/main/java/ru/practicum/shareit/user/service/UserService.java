package ru.practicum.shareit.user.service;

import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.model.User;

public interface UserService {
    User createUser(User user);
    User updateUser(long id, UpdateUserRequest updateUser);
}
