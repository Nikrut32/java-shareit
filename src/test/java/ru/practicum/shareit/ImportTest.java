package ru.practicum.shareit;

import org.springframework.context.annotation.Import;
import ru.practicum.shareit.item.dal.mappers.ItemMapper;
import ru.practicum.shareit.item.dal.mappers.ItemRowMapper;
import ru.practicum.shareit.item.repository.ItemRepositoryImpl;
import ru.practicum.shareit.user.dal.mappers.UserMapper;
import ru.practicum.shareit.user.dal.mappers.UserRowMapper;
import ru.practicum.shareit.user.repository.UserRepositoryImpl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ItemRepositoryImpl.class, UserRepositoryImpl.class, ItemRowMapper.class,
        UserRowMapper.class, ItemMapper.class, UserMapper.class})
public @interface ImportTest {
}
