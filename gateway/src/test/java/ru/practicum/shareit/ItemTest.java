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
import ru.practicum.shareit.item.ItemClient;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@WebMvcTest(ItemController.class)
class ItemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ItemClient itemClient;

    private static final String USER_HEADER = "X-Sharer-User-Id";
    private static final String SERVER_URL = "http://localhost:9090";
    private static final long USER_ID = 1L;

    private ItemClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        client = new ItemClient(SERVER_URL, new RestTemplateBuilder());
        RestTemplate restTemplate = getRestTemplate(client);
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    private ItemDto buildValidDto() {
        ItemDto dto = new ItemDto();
        dto.setName("Drill");
        dto.setDescription("Power drill");
        dto.setAvailable(true);
        return dto;
    }

    @Test
    void getItemsTest() throws Exception {
        when(itemClient.getItems(USER_ID)).thenReturn(ResponseEntity.ok("list"));

        mockMvc.perform(get("/items").header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("list"));

        verify(itemClient).getItems(USER_ID);
    }

    @Test
    void getItemByIdTest() throws Exception {
        when(itemClient.getItemById(5L, USER_ID)).thenReturn(ResponseEntity.ok("item"));

        mockMvc.perform(get("/items/{id}", 5L).header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("item"));

        verify(itemClient).getItemById(5L, USER_ID);
    }

    @Test
    void getItemSearchTextTest() throws Exception {
        when(itemClient.getItemText("drill", USER_ID)).thenReturn(ResponseEntity.ok("found"));

        mockMvc.perform(get("/items/search")
                        .header(USER_HEADER, USER_ID)
                        .param("text", "drill"))
                .andExpect(status().isOk())
                .andExpect(content().string("found"));

        verify(itemClient).getItemText("drill", USER_ID);
    }

    @Test
    void createItemTest() throws Exception {
        ItemDto dto = new ItemDto();
        dto.setName("Drill");
        dto.setDescription("Power drill");
        dto.setAvailable(true);

        when(itemClient.createItem(any(ItemDto.class), eq(USER_ID)))
                .thenReturn(ResponseEntity.ok("created"));

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("created"));

        verify(itemClient).createItem(any(ItemDto.class), eq(USER_ID));
    }

    @Test
    void createCommentTest() throws Exception {
        CommentCreateDto comment = new CommentCreateDto();
        comment.setText("Great!");

        when(itemClient.createComment(eq(USER_ID), any(CommentCreateDto.class), eq(5L)))
                .thenReturn(ResponseEntity.ok("comment"));

        mockMvc.perform(post("/items/{itemId}/comment", 5L)
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isOk())
                .andExpect(content().string("comment"));

        verify(itemClient).createComment(eq(USER_ID), any(CommentCreateDto.class), eq(5L));
    }

    @Test
    void updateItemTest() throws Exception {
        ItemDto dto = new ItemDto();
        dto.setName("New name");

        when(itemClient.updateItem(eq(5L), eq(USER_ID), any(ItemDto.class)))
                .thenReturn(ResponseEntity.ok("updated"));

        mockMvc.perform(patch("/items/{id}", 5L)
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("updated"));

        verify(itemClient).updateItem(eq(5L), eq(USER_ID), any(ItemDto.class));
    }

    @Test
    void deleteItemTest() throws Exception {
        when(itemClient.deleteItem(5L, USER_ID)).thenReturn(ResponseEntity.ok("deleted"));

        mockMvc.perform(delete("/items/{id}", 5L).header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("deleted"));

        verify(itemClient).deleteItem(5L, USER_ID);
    }

    @Test
    void itemInvalidNameThrowsClientTest() {
        ItemDto dto = buildValidDto();
        dto.setName(null);

        assertThrows(ValidationException.class,
                () -> client.createItem(dto, USER_ID));
    }

    @Test
    void itemInvalidDescriptionThrowsClientTest() {
        ItemDto dto = buildValidDto();
        dto.setDescription(null);

        assertThrows(ValidationException.class,
                () -> client.createItem(dto, USER_ID));
    }

    @Test
    void itemNullAvailableThrowsClientTest() {
        ItemDto dto = buildValidDto();
        dto.setAvailable(null);

        assertThrows(ValidationException.class,
                () -> client.createItem(dto, USER_ID));
    }

    @Test
    void getItemsShouldSendGetClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/items"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", String.valueOf(USER_ID)))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.getItems(USER_ID).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void createItemShouldSendPostClientTest() {
        ItemDto dto = buildValidDto();

        mockServer.expect(requestTo(SERVER_URL + "/items"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", String.valueOf(USER_ID)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertTrue(client.createItem(dto, USER_ID).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void getItemTextSendsGetWithParamClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/items/search?text=drill"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.getItemText("drill", USER_ID).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void createCommentShouldSendPostClientTest() {
        CommentCreateDto comment = new CommentCreateDto();
        comment.setText("Great!");

        mockServer.expect(requestTo(SERVER_URL + "/items/5/comment"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertTrue(client.createComment(USER_ID, comment, 5L).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void updateItemShouldSendPatchClientTest() {
        ItemDto dto = buildValidDto();

        mockServer.expect(requestTo(SERVER_URL + "/items/5"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertTrue(client.updateItem(USER_ID, 5L, dto).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void deleteItemShouldSendDeleteClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/items/5"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertTrue(client.deleteItem(5L, USER_ID).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void getItemByIdClientTest() {
        mockServer.expect(requestTo(SERVER_URL + "/items/5"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", String.valueOf(USER_ID)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertTrue(client.getItemById(5L, USER_ID).getStatusCode().is2xxSuccessful());
        mockServer.verify();
    }

    @Test
    void createItemWithBlankName() {
        ItemDto dto = buildValidDto();
        dto.setName("   ");

        assertThrows(ValidationException.class,
                () -> client.createItem(dto, USER_ID));
    }

    @Test
    void createItemWithBlankDescription() {
        ItemDto dto = buildValidDto();
        dto.setDescription("   ");

        assertThrows(ValidationException.class,
                () -> client.createItem(dto, USER_ID));
    }

    private RestTemplate getRestTemplate(ItemClient client) {
        try {
            var field = client.getClass().getSuperclass().getDeclaredField("rest");
            field.setAccessible(true);
            return (RestTemplate) field.get(client);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось получить RestTemplate из BaseClient", e);
        }
    }
}