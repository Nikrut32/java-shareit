package ru.practicum.shareit.user.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictObjectException;
import ru.practicum.shareit.user.dal.mappers.UserMapper;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public UserDto createUser(User user) {
        checkEmail(user.getEmail());
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    @Override
    public UserDto updateUser(long id, UpdateUserRequest updateUser) {
        checkEmail(updateUser.getEmail());
        User user = userRepository.getReferenceById(id);
        if (updateUser.hasName()) {
            user.setName(updateUser.getName());
        }
        if (updateUser.hasEmail()) {
            user.setEmail(updateUser.getEmail());
        }
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    private void checkEmail(String email) {
        if (email == null) {
            return;
        }

        List<User> usersEmail = userRepository.findAll().stream()
                .filter(user -> user.getEmail().equals(email))
                .toList();

        if  (!usersEmail.isEmpty()) {
            throw new ConflictObjectException("Пользователь с этим email уже зарегистрирован");
        }
    }
}
