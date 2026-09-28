package ru.practicum.shareit.user.repository;

import ru.practicum.shareit.user.model.User;

import java.util.List;

public interface UserRepository {
    List<User> getAllUsers();
    User getUserById(long userId);
    User createUser(User user);
    User updateUser(User updateUser);
    void deleteUser(long userId);
    boolean checkUserById(long userId);
}
