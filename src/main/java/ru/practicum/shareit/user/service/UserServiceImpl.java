package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dal.mappers.UserMapper;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto createUser(User user) {
        exceptionUser(user);
        return userRepository.createUser(user);
    }

    @Override
    public UserDto updateUser(long id, UpdateUserRequest updateUser) {
        User user = userMapper.toEntity(userRepository.getUserById(id));
        if (updateUser.hasName()) {
            user.setName(updateUser.getName());
        }
        if (updateUser.hasEmail()) {
            user.setEmail(updateUser.getEmail());
        }
        return userRepository.updateUser(user);
    }

    private void exceptionUser(User user) {
        if (user.getName() == null) {
            log.warn("Валидация не пройдена: логин не был указан");
            throw new ValidationException("Логин не был указан");
        }
        if (user.getName().isBlank()) {
            log.warn("Валидация не пройдена: логин пустой");
            throw new ValidationException("Логин не может быть пустым");
        }
        if (user.getEmail() == null) {
            log.warn("Валидация не пройдена: email не был указан");
            throw new ValidationException("Email не был указан");
        }
        if (user.getEmail().isBlank()) {
            log.warn("Валидация не пройдена: email пустой");
            throw new ValidationException("Указан пустой email");
        }
    }
}
