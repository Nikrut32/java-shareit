package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.practicum.shareit.base.BaseStringResponse;
import ru.practicum.shareit.exception.ConflictObjectException;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.dal.mappers.UserMapper;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserService;
import ru.practicum.shareit.user.service.UserServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserService userServiceMock;

    @InjectMocks
    private UserServiceImpl userService;

    private UserController userController;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Alex")
                .email("alex@example.com")
                .build();

        userDto = UserDto.builder()
                .id(1L)
                .name("Alex")
                .email("alex@example.com")
                .build();

        userController = new UserController(userServiceMock, userRepository, userMapper);
    }

    @Test
    void createUserMappedTest() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        assertEquals(userDto, userService.createUser(user));

        verify(userRepository).findAll();
        verify(userRepository).save(user);
        verify(userMapper).toDto(user);
    }

    @Test
    void createUserEmailTakenConflictTest() {
        User existing = User.builder()
                .id(2L)
                .name("Other")
                .email("alex@example.com")
                .build();

        when(userRepository.findAll()).thenReturn(List.of(existing));

        ConflictObjectException ex = assertThrows(ConflictObjectException.class,
                () -> userService.createUser(user));

        assertEquals("Пользователь с этим email уже зарегистрирован", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void createUserNullEmailNoCheckTest() {
        User noEmailUser = User.builder()
                .id(3L)
                .name("NoEmail")
                .email(null)
                .build();

        UserDto expectedDto = UserDto.builder()
                .id(3L)
                .name("NoEmail")
                .build();

        when(userRepository.save(noEmailUser)).thenReturn(noEmailUser);
        when(userMapper.toDto(noEmailUser)).thenReturn(expectedDto);

        UserDto result = userService.createUser(noEmailUser);

        assertEquals(expectedDto, result);
        verify(userRepository, never()).findAll();
        verify(userRepository).save(noEmailUser);
    }

    @Test
    void updateUserProvidedFieldsTest() {
        User existing = User.builder()
                .id(1L)
                .name("Alex")
                .email("alex@example.com")
                .build();

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Alexander");

        when(userRepository.getReferenceById(1L)).thenReturn(existing);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDto(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return UserDto.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .email(u.getEmail())
                    .build();
        });

        UserDto result = userService.updateUser(1L, request);

        assertEquals("Alexander", result.getName());
        assertEquals("alex@example.com", result.getEmail());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("Alexander", saved.getName());
        assertEquals("alex@example.com", saved.getEmail());
    }

    @Test
    void updateUserEmailOnlyTest() {
        User original = User.builder()
                .id(1L)
                .name("Alex")
                .email("old@example.com")
                .build();

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new@example.com");

        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.getReferenceById(1L)).thenReturn(original);
        when(userRepository.save(original)).thenReturn(original);
        when(userMapper.toDto(original)).thenAnswer(inv -> UserDto.builder()
                .id(original.getId())
                .name(original.getName())
                .email(original.getEmail())
                .build());

        UserDto result = userService.updateUser(1L, request);

        assertEquals("Alex", result.getName());
        assertEquals("new@example.com", result.getEmail());
        verify(userRepository).save(original);
    }

    @Test
    void updateUserNoFieldsNoChangeTest() {
        User existing = User.builder()
                .id(1L).name("Alex").email("alex@example.com").build();

        when(userRepository.getReferenceById(1L)).thenReturn(existing);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDto(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return UserDto.builder()
                    .id(u.getId()).name(u.getName()).email(u.getEmail()).build();
        });

        UserDto result = userService.updateUser(1L, new UpdateUserRequest());

        assertEquals("Alex", result.getName());
        assertEquals("alex@example.com", result.getEmail());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("Alex", captor.getValue().getName());
        assertEquals("alex@example.com", captor.getValue().getEmail());
    }

    @Test
    void updateUserEmailTakenConflictTest() {
        User existing = User.builder()
                .id(2L)
                .name("Other")
                .email("taken@example.com")
                .build();

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("taken@example.com");

        when(userRepository.findAll()).thenReturn(List.of(existing));

        ConflictObjectException ex = assertThrows(ConflictObjectException.class,
                () -> userService.updateUser(1L, request));

        assertEquals("Пользователь с этим email уже зарегистрирован", ex.getMessage());
        verify(userRepository, never()).getReferenceById(anyLong());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserNullEmailNoCheckTest() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("NewName");

        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenAnswer(inv -> UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build());

        UserDto result = userService.updateUser(1L, request);

        assertEquals("NewName", result.getName());
        verify(userRepository, never()).findAll();
        verify(userRepository).save(user);
    }

    @Nested
    class ControllerTests {

        @Test
        void findAllNoEmailTest() {
            User other = User.builder().id(2L).name("Bob").email("bob@example.com").build();

            when(userRepository.findAll()).thenReturn(List.of(user, other));

            List<UserDto> result = userController.findAll();

            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).getId());
            assertEquals("Alex", result.get(0).getName());
            assertNull(result.get(0).getEmail());

            assertEquals(2L, result.get(1).getId());
            assertEquals("Bob", result.get(1).getName());
            assertNull(result.get(1).getEmail());

            verify(userMapper, never()).toDto(any());
        }

        @Test
        void findAllEmptyListTest() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserDto> result = userController.findAll();

            assertTrue(result.isEmpty());
        }

        @Test
        void findByIdMappedTest() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userMapper.toDto(user)).thenReturn(userDto);

            UserDto result = userController.findById(1L);

            assertEquals(userDto, result);
            verify(userRepository).findById(1L);
            verify(userMapper).toDto(user);
        }

        @Test
        void findByIdNotFoundThrowsTest() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            ValidationNotObjectException ex = assertThrows(
                    ValidationNotObjectException.class,
                    () -> userController.findById(99L));

            assertEquals("Пользователем с id: 99 не найден", ex.getMessage());
            verify(userMapper, never()).toDto(any());
        }

        @Test
        void createUserDelegateTest() {
            when(userServiceMock.createUser(user)).thenReturn(userDto);

            UserDto result = userController.createUser(user);

            assertEquals(userDto, result);
            verify(userServiceMock).createUser(user);
        }

        @Test
        void updateUserDelegateTest() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName("Alexander");

            UserDto updatedDto = UserDto.builder()
                    .id(1L).name("Alexander").email("alex@example.com").build();

            when(userServiceMock.updateUser(1L, request)).thenReturn(updatedDto);

            UserDto result = userController.updateUser(1L, request);

            assertEquals(updatedDto, result);
            verify(userServiceMock).updateUser(1L, request);
        }

        @Test
        void deleteUserDeleteByIdAndResponseTest() {
            BaseStringResponse response = userController.deleteUser(1L);

            assertEquals("Пользователь с id: 1 успешно удален", response.getResponse());
            verify(userRepository).deleteById(1L);
        }
    }
}