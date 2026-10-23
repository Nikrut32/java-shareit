package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dal.mappers.UserMapper;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @InjectMocks private UserServiceImpl userService;

    @Test
    void createUserReturnsMappedUser() {
        User user = User.builder().id(1L).name("Alex").email("alex@example.com").build();
        UserDto dto = UserDto.builder().id(1L).name("Alex").email("alex@example.com").build();
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(dto);

        assertEquals(dto, userService.createUser(user));
        verify(userRepository).save(user);
    }

    @Test
    void createUserRejectsBlankName() {
        User user = User.builder().name(" ").email("alex@example.com").build();
        assertThrows(ValidationException.class, () -> userService.createUser(user));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserRejectsMissingEmail() {
        User user = User.builder().name("Alex").email(null).build();
        assertThrows(ValidationException.class, () -> userService.createUser(user));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserChangesOnlyProvidedFields() {
        User user = User.builder().id(1L).name("Alex").email("old@example.com").build();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Alexander");
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenAnswer(invocation -> UserDto.builder()
                .id(user.getId()).name(user.getName()).email(user.getEmail()).build());

        UserDto result = userService.updateUser(1L, request);

        assertEquals("Alexander", result.getName());
        assertEquals("old@example.com", result.getEmail());
        verify(userRepository).save(user);
    }
}
