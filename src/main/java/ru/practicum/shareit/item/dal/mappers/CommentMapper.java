package ru.practicum.shareit.item.dal.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

@Component
@RequiredArgsConstructor
public class CommentMapper {

    public CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorName(comment.getAuthor().getName())
                .item(CommentDto.Item.builder()
                        .id(comment.getItem().getId())
                        .name(comment.getItem().getName())
                        .build())
                .created(comment.getCreated())
                .build();
    }

    public Comment toEntityCreate(CommentCreateDto dto, User booker, Item item) {
        return Comment.builder()
                .id(dto.getId())
                .text(dto.getText())
                .author(booker)
                .item(item)
                .build();
    }
}
