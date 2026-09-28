package ru.practicum.shareit.user.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.base.BaseRepository;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.exception.ConflictObjectException;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl extends BaseRepository<User> implements UserRepository {
    public UserRepositoryImpl(JdbcTemplate jdbc, RowMapper<User> rowMapper) {
        super(jdbc, rowMapper);
    }

    private static final String INSERT_QUERY = "INSERT INTO users (name, email) VALUES (?, ?)";
    private static final String GET_ALL_QUERY = "SELECT * FROM users ORDER BY id";
    private static final String GET_BY_ID_QUERY = "SELECT * FROM users WHERE id = ?";
    private static final String UPDATE_QUERY = "UPDATE users SET name = ?, email = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM users WHERE id = ?";
    private static final String CHECK_USER_BY_EMAIL_QUERY = "SELECT * FROM users WHERE email = ? AND NOT id = ?";

    @Override
    public List<User> getAllUsers() {
        return findAll(GET_ALL_QUERY);
    }

    @Override
    public User getUserById(long userId) {
        Optional<User> user = findByOne(GET_BY_ID_QUERY, userId);

        if (user.isEmpty()) {
            throw new ValidationNotObjectException("Пользователь с id: " + userId + " не найден");
        }

        return user.get();
    }

    @Override
    public User createUser(User user) {
        if (checkUserByEmail(user.getEmail(), 0)) {
            throw new ConflictObjectException("Пользователь с таким email: " + user.getEmail() + " уже зарегистрирован");
        }
        long id = insert(INSERT_QUERY, user.getName(), user.getEmail());
        user.setId(id);
        return user;
    }

    @Override
    public User updateUser(User updateUser) {
        if (checkUserByEmail(updateUser.getEmail(), updateUser.getId())) {
            throw new ConflictObjectException("Пользователь с таким email: " + updateUser.getEmail() + " уже зарегистрирован");
        }
        update(UPDATE_QUERY, updateUser.getName(), updateUser.getEmail(), updateUser.getId());
        return updateUser;
    }

    @Override
    public void deleteUser(long userId) {
        delete(checkUserById(userId),  DELETE_QUERY, userId);
    }

    @Override
    public boolean checkUserById(long userId) {
        return findByOne(GET_BY_ID_QUERY, userId).isPresent();
    }

    private boolean checkUserByEmail(String email, long id) {
        return findByOne(CHECK_USER_BY_EMAIL_QUERY, email, id).isPresent();
    }
}
