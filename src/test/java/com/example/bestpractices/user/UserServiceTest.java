package com.example.bestpractices.user;

import com.example.bestpractices.user.dto.CreateUserRequest;
import com.example.bestpractices.user.dto.UpdateUserRequest;
import com.example.bestpractices.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Best practices demonstrated:
 * - Use @ExtendWith(MockitoExtension.class) — lighter than @SpringBootTest, no Spring context needed
 * - @InjectMocks creates the class under test; @Mock injects its dependencies
 * - Test behaviour, not implementation: assert on return values and thrown exceptions
 * - Use AssertJ (assertThat) for fluent, readable assertions
 * - One logical assertion per test; arrange-act-assert structure
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("jdoe");
        sampleUser.setEmail("jdoe@example.com");
        sampleUser.setFirstName("John");
        sampleUser.setLastName("Doe");
        sampleUser.setActive(true);
    }

    @Test
    void findById_existingUser_returnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.findById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("jdoe");
    }

    @Test
    void findById_missingUser_throwsNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_duplicateUsername_throwsIllegalArgument() {
        when(userRepository.existsByUsername("jdoe")).thenReturn(true);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("jdoe");
        request.setEmail("other@example.com");
        request.setFirstName("Jane");
        request.setLastName("Doe");

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_validRequest_savesAndReturnsResponse() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setFirstName("New");
        request.setLastName("User");

        UserResponse response = userService.create(request);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getUsername()).isEqualTo("newuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void update_partialFields_onlyUpdatesProvidedFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Johnny");

        UserResponse response = userService.update(1L, request);

        assertThat(response.getFirstName()).isEqualTo("Johnny");
        assertThat(response.getLastName()).isEqualTo("Doe"); // unchanged
    }

    @Test
    void delete_missingUser_throwsNotFoundException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).deleteById(any());
    }
}
