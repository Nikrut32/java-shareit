package ru.practicum.shareit.item.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.base.BaseRepository;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.item.dal.mappers.ItemMapper;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ItemRepositoryImpl extends BaseRepository<Item> implements ItemRepository {
    @Autowired
    private ItemMapper itemMapper;

    public ItemRepositoryImpl(JdbcTemplate jdbc, RowMapper<Item> rowMapper) {
        super(jdbc, rowMapper);
    }

    private static final String GET_BY_OWNER_ID_QUERY = "SELECT * FROM items WHERE owner_id = ?";
    private static final String GET_ALL_QUERY = "SELECT * FROM items";
    private static final String GET_BY_ID_QUERY = "SELECT * FROM items WHERE id = ?";
    private static final String INSERT_QuERY = "INSERT INTO items (name, description, available, owner_id, request_id) " +
            "VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_QUERY = "UPDATE items SET name = ?, description = ?, available = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM items WHERE id = ?";

    @Override
    public List<Item> getAllItems(long ownerId) {
        return findAll(GET_BY_OWNER_ID_QUERY, ownerId);
    }

    @Override
    public Item getItemById(Long itemId) {
        Optional<Item> item = findByOne(GET_BY_ID_QUERY, itemId);

        if (item.isEmpty()) {
            throw new ValidationNotObjectException("Предмет с id: " + itemId + " не найден");
        }

        return item.get();
    }

    @Override
    public List<Item> getItemSearchText(String searchText) {
        return findAll(GET_ALL_QUERY).stream()
                .filter(item -> (item.getDescription().toLowerCase().contains(searchText.toLowerCase())
                        || item.getName().toLowerCase().contains(searchText.toLowerCase())) && item.getAvailable())
                .collect(Collectors.toList());
    }

    @Override
    public Item createItem(ItemDto item, Long ownerId, Long requestId) {
        long id = insert(INSERT_QuERY, item.getName(), item.getDescription(), item.getAvailable(), ownerId, requestId);
        item.setId(id);
        return itemMapper.toEntity(item, ownerId, requestId);
    }

    @Override
    public ItemDto updateItem(ItemDto updateItem) {
        update(UPDATE_QUERY, updateItem.getName(), updateItem.getDescription(), updateItem.getAvailable(), updateItem.getId());

        return updateItem;
    }

    @Override
    public void deleteItem(Long itemId) {
        delete(checkItem(itemId), DELETE_QUERY, itemId);
    }

    @Override
    public boolean checkOwner(long itemId, long ownerId) {
        if (checkItem(itemId)) {
            Item item = findByOne(GET_BY_ID_QUERY, itemId).get();
            return  item.getOwner().getId() == ownerId;
        } else {
            throw new ValidationNotObjectException("Предмет с id: " + itemId + " не найден");
        }
    }

    private boolean checkItem(long itemId) {
        return findByOne(GET_BY_ID_QUERY, itemId).isPresent();
    }
}
