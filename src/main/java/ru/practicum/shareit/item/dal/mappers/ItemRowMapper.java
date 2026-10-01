package ru.practicum.shareit.item.dal.mappers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.repository.UserRepository;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class ItemRowMapper implements RowMapper<Item> {
    @Autowired
    private UserRepository userRepository;

    @Override
    public Item mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Item.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .available(rs.getBoolean("available"))
                .owner(userRepository.getUserById(rs.getLong("owner_id")))
                .request(ItemRequest.builder().id(rs.getLong("request_id")).build())
                .countRental(rs.getLong("count_rental"))
                .build();
    }
}
