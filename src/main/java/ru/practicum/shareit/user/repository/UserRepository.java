package ru.practicum.shareit.user.repository;

import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;

public interface UserRepository {
    List<User> getAllUsers();

    UserDto getUserById(long userId);

    UserDto createUser(User user);

    UserDto updateUser(User updateUser);

    void deleteUser(long userId);

    boolean checkUserById(long userId);
}
