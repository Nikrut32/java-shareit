package ru.practicum.shareit.request.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.request.dal.mappers.RequestMapper;
import ru.practicum.shareit.request.dto.GetItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository itemRequestRepository;
    private final RequestMapper requestMapper;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public ItemRequestDto createRequest(ItemRequest itemRequest, Long ownerId) {
        itemRequest.setOwner(userRepository.findById(ownerId)
                .orElseThrow(() -> new ValidationException("Ваш пользователь не зарегистрирован")));

        return requestMapper.toDto(itemRequestRepository.save(itemRequest));
    }

    @Override
    public List<GetItemRequestDto> getRequestsOwnerId(Long ownerId) {
        if (!userRepository.existsById(ownerId)) {
            throw new ValidationException("Ваш пользователь не зарегистрирован");
        }

        return itemRequestRepository.findByOwnerId(ownerId).stream()
                .map(requestMapper::toGetRequestDto)
                .sorted(Comparator.comparing(GetItemRequestDto::getCreated).reversed())
                .toList();
    }

    @Override
    public List<ItemRequestDto> getRequestsAll(Long ownerId) {
        if (!userRepository.existsById(ownerId)) {
            throw new ValidationException("Ваш пользователь не зарегистрирован");
        }

        return  itemRequestRepository.findAll().stream()
                .map(requestMapper::toDto)
                .sorted(Comparator.comparing(ItemRequestDto::getCreated).reversed())
                .toList();
    }

    @Override
    public GetItemRequestDto getRequestById(Long requestId) {
        return requestMapper.toGetRequestDto(itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new ValidationException("Запрос не найден")));
    }
}
