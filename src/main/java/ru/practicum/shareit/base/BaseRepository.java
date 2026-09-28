package ru.practicum.shareit.base;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import ru.practicum.shareit.exception.DataProcessingException;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class BaseRepository<T> {
    private final JdbcTemplate jdbc;
    private final RowMapper<T> rowMapper;

    protected List<T> findAll(String query, Object... params) {
        return jdbc.query(query, rowMapper, params);
    }

    protected Optional<T> findByOne(String query, Object... params) {
        try {
            T object = jdbc.queryForObject(query, rowMapper, params);
            return Optional.ofNullable(object);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    protected long insert(String query, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connect -> {
            PreparedStatement ps = connect.prepareStatement(query, new String[]{"id"});

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            return ps;
        }, keyHolder);

        Long id = keyHolder.getKeyAs(Long.class);

        if (id != null) {
            return id;
        } else {
            throw new DataProcessingException("Сохранить данные не удалось");
        }
    }

    protected void update(String query, Object... params) {
        int result = jdbc.update(query, params);

        if (result != 1) {
            throw new DataProcessingException("Обновить данные не удалось");
        }
    }

    protected void delete(boolean check, String query, Object... params) {
        if (!check) {
            throw new DataProcessingException("Удалить данные не удалось");
        }

        jdbc.update(query, params);
    }
}
