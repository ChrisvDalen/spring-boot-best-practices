package com.example.bestpractices.user;

import com.example.bestpractices.user.dto.CreateUserRequest;
import com.example.bestpractices.user.dto.UpdateUserRequest;
import com.example.bestpractices.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Best practices demonstrated:
 * - @RequiredArgsConstructor for constructor injection (no @Autowired on fields)
 * - @Transactional(readOnly = true) as class default; override with readOnly = false on writes
 * - Service layer owns mapping from entity → DTO; controllers never touch entities
 * - Spring Cache annotations: @Cacheable, @CachePut, @CacheEvict
 * - Validate uniqueness before persisting, throw domain exception on conflict
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public Page<UserResponse> findAllActive(Pageable pageable) {
        return userRepository.findAllActive(pageable).map(this::toResponse);
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponse findById(Long id) {
        log.debug("Fetching user {}", id);
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User saved = userRepository.save(user);
        log.info("Created user id={} username={}", saved.getId(), saved.getUsername());
        return toResponse(saved);
    }

    @Transactional
    @CachePut(value = "users", key = "#id")
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getActive() != null) user.setActive(request.getActive());

        return toResponse(userRepository.save(user));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
        log.info("Deleted user id={}", id);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
