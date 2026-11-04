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
import ru.practicum.shareit.request.RequestClient;
import ru.practicum.shareit.request.RequestController;
import ru.practicum.shareit.request.dto.CreateRequestDto;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@WebMvcTest(RequestController.class)
class RequestTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RequestClient requestClient;

    private static final String USER_HEADER = "X-Sharer-User-Id";
    private static final String SERVER_URL = "http://localhost:9090";
    private static final long USER_ID = 1L;

    private RequestClient client;
    private MockRestServiceServer mockServer;

    @Test
    void createRequestTest() throws Exception {
        CreateRequestDto dto = new CreateRequestDto();
        dto.setDescription("Need a drill");

        when(requestClient.createRequest(any(CreateRequestDto.class), eq(USER_ID)))
                .thenReturn(ResponseEntity.ok("created"));

        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("created"));

        verify(requestClient).createRequest(any(CreateRequestDto.class), eq(USER_ID));
    }

    @BeforeEach
    void setUp() {
        client = new RequestClient(SERVER_URL, new RestTemplateBuilder());
        mockServer = MockRestServiceServer.bindTo(getRestTemplate(client)).build();
    }

    private CreateRequestDto validDto() {
        CreateRequestDto dto = new CreateRequestDto();
        dto.setDescription("Need a drill");
        return dto;
    }

    @Test
    void getRequestsOwnerIdTest() throws Exception {
        when(requestClient.getRequests(USER_ID, ""))
                .thenReturn(ResponseEntity.ok("ownList"));

        mockMvc.perform(get("/requests")
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("ownList"));

        verify(requestClient).getRequests(USER_ID, "");
    }

    @Test
    void getRequestsAllTest() throws Exception {
        when(requestClient.getRequests(USER_ID, "/all"))
                .thenReturn(ResponseEntity.ok("allList"));

        mockMvc.perform(get("/requests/all")
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("allList"));

        verify(requestClient).getRequests(USER_ID, "/all");
    }

    @Test
    void getRequestByIdTest() throws Exception {
        when(requestClient.getRequests(USER_ID, "/5"))
                .thenReturn(ResponseEntity.ok("request"));

        mockMvc.perform(get("/requests/{requestId}", 5L)
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("request"));

        verify(requestClient).getRequests(USER_ID, "/5");
    }

    @Test
    void createRequestWithNullDescriptionClientTest() {
        CreateRequestDto dto = new CreateRequestDto();
        dto.setDescription(null);

        assertThrows(ValidationException.class,
                () -> client.createRequest(dto, USER_ID));
    }

    @Test
    void createRequestWithBlankDescriptionClientTest() {
        CreateRequestDto dto = new CreateRequestDto();
        dto.setDescription("   ");

        assertThrows(ValidationException.class,
                () -> client.createRequest(dto, USER_ID));
    }

    @Test
    void createRequestClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/requests"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", String.valueOf(USER_ID)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertTrue(client.createRequest(validDto(), USER_ID).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void getRequestsClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/requests"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", String.valueOf(USER_ID)))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.getRequests(USER_ID, "").getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void getRequestsWithPathClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/requests/all"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.getRequests(USER_ID, "/all").getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    private RestTemplate getRestTemplate(RequestClient client) {
        try {
            var field = client.getClass().getSuperclass().getDeclaredField("rest");
            field.setAccessible(true);
            return (RestTemplate) field.get(client);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось получить RestTemplate из BaseClient", e);
        }
    }
}