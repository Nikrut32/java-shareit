package ru.practicum.shareit;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.exception.ConflictObjectException;
import ru.practicum.shareit.exception.ValidationNotObjectException;
import ru.practicum.shareit.item.dal.mappers.ItemRowMapper;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepositoryImpl;
import ru.practicum.shareit.user.dal.mappers.UserRowMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepositoryImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import({ItemRepositoryImpl.class, UserRepositoryImpl.class, ItemRowMapper.class, UserRowMapper.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ShareItTests {
	private final ItemRepositoryImpl itemRepository;
	private final UserRepositoryImpl userRepository;

	private User buildUser(String name) {
		return User.builder().name(name).email("ivan@mail.ru").build();
	}

	private ItemDto buildDto(String name, String description, Boolean available) {
		return ItemDto.builder().name(name).description(description).available(available).build();
	}

	@Test
	void createUserSavesAndReturnsUserWithIdTest() {
		User created = userRepository.createUser(buildUser("Ivan"));

		assertThat(created.getId()).isPositive();
		assertThat(userRepository.getUserById(created.getId())).isEqualTo(created);
	}

	@Test
	void createUserThrowsConflictWhenEmailExistsTest() {
		userRepository.createUser(buildUser("Ivan"));

		assertThatThrownBy(() -> userRepository.createUser(buildUser("Petr")))
				.isInstanceOf(ConflictObjectException.class);
	}

	@Test
	void getUserByIdThrowsWhenNotFoundTest() {
		assertThatThrownBy(() -> userRepository.getUserById(999L))
				.isInstanceOf(ValidationNotObjectException.class);
	}

	@Test
	void updateUserUpdatesFieldsTest() {
		User created = userRepository.createUser(buildUser("Ivan"));

		created.setName("Updated");
		created.setEmail("new@mail.ru");
		userRepository.updateUser(created);

		User updated = userRepository.getUserById(created.getId());
		assertThat(updated.getName()).isEqualTo("Updated");
		assertThat(updated.getEmail()).isEqualTo("new@mail.ru");
	}

	@Test
	void deleteUserRemovesUserTest() {
		User created = userRepository.createUser(buildUser("Ivan"));

		userRepository.deleteUser(created.getId());

		assertThat(userRepository.checkUserById(created.getId())).isFalse();
	}

	@Test
	void createItemSavesAndReturnsItemTest() {
		Item created = itemRepository.createItem(
				buildDto("Drill", "Powerful drill", true), 1L, null);

		assertThat(created.getId()).isPositive();
		assertThat(itemRepository.getItemById(created.getId()).getName()).isEqualTo("Drill");
		assertThat(created.getOwnerId()).isEqualTo(1);
	}

	@Test
	void getItemByIdThrowsWhenNotFoundTest() {
		assertThatThrownBy(() -> itemRepository.getItemById(999L))
				.isInstanceOf(ValidationNotObjectException.class);
	}

	@Test
	void getAllItemsReturnsOnlyOwnerItemsTest() {
		itemRepository.createItem(buildDto("A", "desc A", true), 1L, null);
		itemRepository.createItem(buildDto("B", "desc B", true), 1L, null);
		itemRepository.createItem(buildDto("C", "desc C", true), 2L, null);

		List<Item> ownerItems = itemRepository.getAllItems(1);

		assertThat(ownerItems).extracting(Item::getName).containsExactlyInAnyOrder("Перфоратор Makita", "A", "B");
	}

	@Test
	void searchTextNameIgnoreCaseExcludesUnavailableTest() {
		itemRepository.createItem(buildDto("Drill", "Powerful", true), 1L, null);
		itemRepository.createItem(buildDto("Drill Pro", "Powerful", false), 1L, null);

		List<Item> found = itemRepository.getItemSearchText("dril");

		assertThat(found).hasSize(1);
		assertThat(found.getFirst().getName()).isEqualTo("Drill");
	}

	@Test
	void updateItemChangesFieldsTest() {
		Item created = itemRepository.createItem(
				buildDto("Drill", "Powerful", true), 1L, null);

		ItemDto update = ItemDto.builder()
				.id(created.getId())
				.name("New Drill")
				.description("Updated")
				.available(false)
				.build();
		itemRepository.updateItem(update);

		Item fromDb = itemRepository.getItemById(created.getId());
		assertThat(fromDb.getName()).isEqualTo("New Drill");
		assertThat(fromDb.getAvailable()).isFalse();
	}

	@Test
	void checkOwnerResultIsCorrectTest() {
		Item created = itemRepository.createItem(
				buildDto("Drill", "Powerful", true), 1L, null);

		assertThat(itemRepository.checkOwner(created.getId(), 1)).isTrue();
		assertThat(itemRepository.checkOwner(created.getId(), 2)).isFalse();
	}

}
