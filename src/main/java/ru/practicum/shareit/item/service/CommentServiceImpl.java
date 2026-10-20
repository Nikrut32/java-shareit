package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.Status;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.item.dal.mappers.CommentMapper;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.repository.CommentRepository;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService{
    private final CommentMapper commentMapper;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BookingRepository bookingRepository;

    @Override
    public CommentDto createComment(CommentCreateDto commentCreate, long bookerId, long itemId) {
        Comment comment = commentMapper.toEntityCreate(commentCreate,
                userRepository.findById(bookerId)
                        .orElseThrow(() -> new ValidationNotObjectException("Ваш пользователь не зарегистрирован")),
                itemRepository.findById(itemId)
                        .orElseThrow(() -> new ValidationNotObjectException("Такого предмета не существует")));

        List<Booking> bookingsItem = bookingRepository.findAll().stream()
                .filter(booking -> booking.getItem().getId() == itemId
                    && booking.getBooker().getId() == bookerId
                    && booking.getStatus() == Status.APPROVED
                    && booking.getEnd().isBefore(LocalDateTime.now()))
                .toList();

        if (bookingsItem.isEmpty()) {
            throw new ValidationException("Вы не можете оставить комментарий");
        }

        return commentMapper.toDto(commentRepository.save(comment));
    }
}
