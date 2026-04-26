package com.example.bestpractices.user;

import com.example.bestpractices.metrics.UserMetrics;
import com.example.bestpractices.messaging.UserEventType;
import com.example.bestpractices.outbox.OutboxEventService;
import com.example.bestpractices.user.dto.CreateUserRequest;
import com.example.bestpractices.user.dto.UpdateUserRequest;
import com.example.bestpractices.user.dto.UserResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Best practices demonstrated:
 * - Use @ExtendWith(MockitoExtension.class) — lighter than @SpringBootTest, no Spring context needed
 * - New dependencies (OutboxEventService, UserMetrics) declared as @Mock so Mockito
 *   wires them via constructor injection — @InjectMocks still has a single point of control
 * - @Spy on UserMetrics with a real SimpleMeterRegistry lets us verify metric increments
 *   without needing a full Spring context
 * - Verify outbox interaction: every write operation must produce an outbox event
 * - Test behaviour, not implementation; assert on return values and thrown exceptions
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @Spy
    private UserMetrics userMetrics = new UserMetrics(new SimpleMeterRegistry());

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
        verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void create_validRequest_savesAndPublishesOutboxEvent() {
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
        verify(outboxEventService).saveEvent(eq("User"), eq(42L), eq(UserEventType.CREATED), any());
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
        verify(outboxEventService).saveEvent(eq("User"), eq(1L), eq(UserEventType.UPDATED), any());
    }

    @Test
    void delete_missingUser_throwsNotFoundException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).deleteById(any());
        verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    void delete_existingUser_publishesDeletedOutboxEvent() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
        verify(outboxEventService).saveEvent(eq("User"), eq(1L), eq(UserEventType.DELETED), any());
    }
}
