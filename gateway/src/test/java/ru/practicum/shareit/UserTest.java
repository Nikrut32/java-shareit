package ru.practicum.shareit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.UserClient;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.dto.UserDto;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@WebMvcTest(UserController.class)
class UserTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserClient userClient;

    private static final String SERVER_URL = "http://localhost:9090";
    private static final long USER_ID = 1L;

    private UserClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        client = new UserClient(SERVER_URL, new RestTemplateBuilder());
        mockServer = MockRestServiceServer.bindTo(getRestTemplate(client)).build();
    }

    private UserDto validDto() {
        UserDto dto = new UserDto();
        dto.setName("Ivan");
        dto.setEmail("ivan@mail.ru");
        return dto;
    }

    private UserDto buildDto() {
        UserDto dto = new UserDto();
        dto.setName("Ivan");
        dto.setEmail("ivan@mail.ru");
        return dto;
    }

    @Test
    void getUsersTest() throws Exception {
        when(userClient.getUsers()).thenReturn(ResponseEntity.ok("list"));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().string("list"));

        verify(userClient).getUsers();
    }

    @Test
    void getUserByIdTest() throws Exception {
        when(userClient.getUserById(5L)).thenReturn(ResponseEntity.ok("user"));

        mockMvc.perform(get("/users/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(content().string("user"));

        verify(userClient).getUserById(5L);
    }

    @Test
    void createUserTest() throws Exception {
        UserDto dto = buildDto();
        when(userClient.createUser(any(UserDto.class)))
                .thenReturn(ResponseEntity.ok("created"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("created"));

        verify(userClient).createUser(any(UserDto.class));
    }

    @Test
    void updateUserTest() throws Exception {
        UserDto dto = buildDto();
        when(userClient.updateUser(any(UserDto.class), eq(5L)))
                .thenReturn(ResponseEntity.ok("updated"));

        mockMvc.perform(patch("/users/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("updated"));

        verify(userClient).updateUser(any(UserDto.class), eq(5L));
    }

    @Test
    void deleteUserByIdTest() throws Exception {
        when(userClient.deleteUserById(5L)).thenReturn(ResponseEntity.ok("deleted"));

        mockMvc.perform(delete("/users/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(content().string("deleted"));

        verify(userClient).deleteUserById(5L);
    }

    @Test
    void getUsersClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/users"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.getUsers().getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void getUserByIdClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/users/5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertTrue(client.getUserById(5L).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void createUserClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertTrue(client.createUser(validDto()).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void updateUserClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/users/5"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertTrue(client.updateUser(validDto(), 5L).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void deleteUserByIdClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/users/5"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertTrue(client.deleteUserById(5L).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void createUserWithNullNameClientTest() {
        UserDto dto = validDto();
        dto.setName(null);

        assertThrows(ValidationException.class,
                () -> client.createUser(dto));
    }

    @Test
    void createUserWithBlankNameClientTest() {
        UserDto dto = validDto();
        dto.setName("   ");

        assertThrows(ValidationException.class,
                () -> client.createUser(dto));
    }

    @Test
    void createUserWithNullEmailClientTest() {
        UserDto dto = validDto();
        dto.setEmail(null);

        assertThrows(ValidationException.class,
                () -> client.createUser(dto));
    }

    @Test
    void createUserWithBlankEmailClientTest() {
        UserDto dto = validDto();
        dto.setEmail("   ");

        assertThrows(ValidationException.class,
                () -> client.createUser(dto));
    }

    private RestTemplate getRestTemplate(UserClient client) {
        try {
            var field = client.getClass().getSuperclass().getDeclaredField("rest");
            field.setAccessible(true);
            return (RestTemplate) field.get(client);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось получить RestTemplate из BaseClient", e);
        }
    }
}