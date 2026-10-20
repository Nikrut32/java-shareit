package ru.practicum.shareit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.Status;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.item.dal.mappers.CommentMapper;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.CommentServiceImpl;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.CommentRepository;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {
    @Mock private CommentMapper commentMapper;
    @Mock private CommentRepository commentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private BookingRepository bookingRepository;
    @InjectMocks private CommentServiceImpl commentService;

    private User booker;
    private Item item;
    private CommentCreateDto request;
    private Comment comment;

    @BeforeEach
    void setUp() {
        booker = User.builder().id(2L).name("Booker").email("booker@example.com").build();
        item = Item.builder().id(10L).name("Drill").build();
        request = CommentCreateDto.builder().text("Works well").build();
        comment = Comment.builder().id(5L).author(booker).item(item).text("Works well").build();
    }

    @Test
    void createCommentSavesCommentAfterCompletedApprovedBooking() {
        Booking completed = Booking.builder().item(item).booker(booker).status(Status.APPROVED)
                .end(LocalDateTime.now().minusDays(1)).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentMapper.toEntityCreate(request, booker, item)).thenReturn(comment);
        when(bookingRepository.findAll()).thenReturn(List.of(completed));
        when(commentRepository.save(comment)).thenReturn(comment);
        CommentDto dto = CommentDto.builder().id(5L).text("Works well").build();
        when(commentMapper.toDto(comment)).thenReturn(dto);

        assertEquals(dto, commentService.createComment(request, 2L, 10L));
        verify(commentRepository).save(comment);
    }

    @Test
    void createCommentRejectsBookerWithoutCompletedBooking() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentMapper.toEntityCreate(request, booker, item)).thenReturn(comment);
        when(bookingRepository.findAll()).thenReturn(List.of());

        assertThrows(ValidationException.class, () -> commentService.createComment(request, 2L, 10L));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void createCommentRejectsUnknownUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ValidationNotObjectException.class,
                () -> commentService.createComment(request, 2L, 10L));
    }
}
