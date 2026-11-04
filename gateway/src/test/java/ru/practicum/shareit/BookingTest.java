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
import ru.practicum.shareit.booking.BookingClient;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.exception.DataProcessingException;
import ru.practicum.shareit.exception.ValidationException;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@WebMvcTest(BookingController.class)
class BookingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingClient bookingClient;

    private static final String USER_HEADER = "X-Sharer-User-Id";
    private static final String SERVER_URL = "http://localhost:9090";
    private static final long USER_ID = 1L;

    private BookingClient client;
    private MockRestServiceServer mockServer;

    private BookItemRequestDto buildDto() {
        BookItemRequestDto dto = new BookItemRequestDto();
        dto.setItemId(2L);
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));
        return dto;
    }

    @BeforeEach
    void setUp() {
        client = new BookingClient(SERVER_URL, new RestTemplateBuilder());
        RestTemplate restTemplate = getRestTemplate(client);
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void createBookingTest() throws Exception {
        BookItemRequestDto dto = buildDto();
        when(bookingClient.createBooking(eq(USER_ID), any(BookItemRequestDto.class)))
                .thenReturn(ResponseEntity.ok("created"));

        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("created"));

        verify(bookingClient).createBooking(eq(USER_ID), any(BookItemRequestDto.class));
    }

    @Test
    void updateStatusTest() throws Exception {
        when(bookingClient.updateStatus(10L, true, USER_ID))
                .thenReturn(ResponseEntity.ok("approved"));

        mockMvc.perform(patch("/bookings/{id}", 10L)
                        .header(USER_HEADER, USER_ID)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string("approved"));

        verify(bookingClient).updateStatus(10L, true, USER_ID);
    }

    @Test
    void getBookingByIdTest() throws Exception {
        when(bookingClient.getBookingById(10L, USER_ID))
                .thenReturn(ResponseEntity.ok("booking"));

        mockMvc.perform(get("/bookings/{id}", 10L)
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("booking"));

        verify(bookingClient).getBookingById(10L, USER_ID);
    }

    @Test
    void getBookingsByBookerTest() throws Exception {
        when(bookingClient.getBookingsByUserId(USER_ID, BookingState.ALL, ""))
                .thenReturn(ResponseEntity.ok("list"));

        mockMvc.perform(get("/bookings")
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("list"));

        verify(bookingClient).getBookingsByUserId(USER_ID, BookingState.ALL, "");
    }

    @Test
    void getBookingsByOwnerTest() throws Exception {
        when(bookingClient.getBookingsByUserId(USER_ID, BookingState.ALL, "/owner"))
                .thenReturn(ResponseEntity.ok("ownerList"));

        mockMvc.perform(get("/bookings/owner")
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("ownerList"));

        verify(bookingClient).getBookingsByUserId(USER_ID, BookingState.ALL, "/owner");
    }

    @Test
    void createBookingNullStartThrowsClientTest() {
        BookItemRequestDto dto = new BookItemRequestDto();
        dto.setEnd(LocalDateTime.now().plusDays(1));

        assertThrows(DataProcessingException.class,
                () -> client.createBooking(USER_ID, dto));
    }

    @Test
    void createBookingStartAfterEndThrowsClientTest() {
        BookItemRequestDto dto = new BookItemRequestDto();
        dto.setStart(LocalDateTime.now().plusDays(2));
        dto.setEnd(LocalDateTime.now().plusDays(1));

        assertThrows(ValidationException.class,
                () -> client.createBooking(USER_ID, dto));
    }

    @Test
    void createBookingShouldSendPostClientTest() {
        BookItemRequestDto dto = new BookItemRequestDto();
        dto.setItemId(2L);
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        mockServer.expect(requestTo(SERVER_URL + "/bookings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", String.valueOf(USER_ID)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        ResponseEntity<Object> response = client.createBooking(USER_ID, dto);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void createBookingSendsPostClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/bookings/10?approved=true"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        ResponseEntity<Object> response = client.updateStatus(10L, true, USER_ID);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void getBookingsByUserIdSendsGetClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/bookings/owner?state=FUTURE"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        ResponseEntity<Object> response = client.getBookingsByUserId(USER_ID, BookingState.FUTURE, "/owner");

        assertTrue(response.getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void getBookingByIdClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/bookings/10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", String.valueOf(USER_ID)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        ResponseEntity<Object> response = client.getBookingById(10L, USER_ID);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void createBookingWithNullEndClientTest() {
        BookItemRequestDto dto = new BookItemRequestDto();
        dto.setStart(LocalDateTime.now().plusDays(1));

        assertThrows(DataProcessingException.class,
                () -> client.createBooking(USER_ID, dto));
    }

    private RestTemplate getRestTemplate(BookingClient client) {
        try {
            var field = client.getClass().getSuperclass().getDeclaredField("rest");
            field.setAccessible(true);
            return (RestTemplate) field.get(client);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось получить RestTemplate из BaseClient", e);
        }
    }
}