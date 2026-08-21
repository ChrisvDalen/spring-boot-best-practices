package com.example.bestpractices.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Best practices demonstrated:
 * - @DataJpaTest is a slice test: only JPA components are loaded, uses an embedded H2 DB
 * - @Import(JpaConfig.class) brings in @EnableJpaAuditing so @CreatedDate / @LastModifiedDate work
 * - Test custom repository methods (@Query, derived finders) — JPA generates the SQL so test it
 * - Verify uniqueness constraints by checking existsBy* before testing save conflicts
 */
@DataJpaTest
@Import(com.example.bestpractices.config.JpaConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User buildUser(String username, String email) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setFirstName("Test");
        u.setLastName("User");
        return u;
    }

    @Test
    void findByUsername_existingUser_returnsUser() {
        userRepository.save(buildUser("alice", "alice@example.com"));

        Optional<User> found = userRepository.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void findByUsername_unknownUsername_returnsEmpty() {
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void existsByEmail_afterSave_returnsTrue() {
        userRepository.save(buildUser("bob", "bob@example.com"));

        assertThat(userRepository.existsByEmail("bob@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    void findAllActive_excludesInactiveUsers() {
        User active = buildUser("charlie", "charlie@example.com");
        User inactive = buildUser("dave", "dave@example.com");
        inactive.setActive(false);

        userRepository.save(active);
        userRepository.save(inactive);

        Page<User> result = userRepository.findAllActive(PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(User::getUsername)
                .containsExactly("charlie")
                .doesNotContain("dave");
    }

    @Test
    void save_setsAuditTimestamps() {
        User saved = userRepository.save(buildUser("eve", "eve@example.com"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
