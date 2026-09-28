package ru.practicum.shareit.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.base.BaseStringResponse;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(path = "/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping
    public List<UserDto> findAll() {
        log.info("Получен запрос GET /users.");
        return userRepository.getAllUsers().stream().map(us -> {
            return UserDto.builder().id(us.getId()).name(us.getName()).build();
        })
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable long id) {
        log.info("Получен запрос GET /users/{id}} с параметром id={}", id);
        return userRepository.getUserById(id);
    }

    @PostMapping
    public User createUser(@RequestBody @Valid User user) {
        log.info("Получен запрос POST /users на создание пользователя: {}", user);
        User createdUser = userService.createUser(user);
        log.info("Пользователь успешно создан с id={}: {}", createdUser.getId(), createdUser.getName());
        return createdUser;
    }

    @PatchMapping("/{id}")
    public User updateUser(@PathVariable long id, @RequestBody UpdateUserRequest updateUser) {
        log.info("Получен запрос PATCH /users на обновление пользователя с id={}", id);
        return userService.updateUser(id, updateUser);
    }

    @DeleteMapping("/{id}")
    public BaseStringResponse deleteUser(@PathVariable long id) {
        log.info("Получен запрос DELETE /users/{} на удаление пользователя", id);
        userRepository.deleteUser(id);
        log.info("Пользователь с id={} успешно удален", id);
        return new BaseStringResponse("Пользователь с id: " + id + " успешно удален");
    }
}
