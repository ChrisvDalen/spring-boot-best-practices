package com.example.bestpractices.user;

import com.example.bestpractices.idempotency.IdempotencyService;
import com.example.bestpractices.user.dto.CreateUserRequest;
import com.example.bestpractices.user.dto.UserResponse;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Best practices demonstrated:
 * - @WebMvcTest is a slice test: only the web layer is loaded (fast, no DB or full context)
 * - @MockitoBean replaces the real service/filter dependencies in the Spring context; the
 *   IdempotencyService mock is required because IdempotencyFilter is a @Component that
 *   @WebMvcTest picks up and tries to wire
 * - @Import(SecurityConfig.class) tests with the real security rules
 * - @WithMockUser provides an authenticated principal for write-operation tests without
 *   needing a running auth server
 * - Test HTTP contract: status codes, Location header on 201, response body shape
 * - Test validation rejection: send a bad request and assert 400 with field errors in the body
 */
@WebMvcTest(UserController.class)
@Import(com.example.bestpractices.security.SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    // IdempotencyFilter is a @Component loaded by @WebMvcTest; it needs this mock
    @MockitoBean
    private IdempotencyService idempotencyService;

    private UserResponse sampleResponse() {
        return UserResponse.builder()
                .id(1L).username("jdoe").email("jdoe@example.com")
                .firstName("John").lastName("Doe").active(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    @Test
    void listUsers_returns200WithPage() throws Exception {
        when(userService.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("jdoe"));
    }

    @Test
    void getUser_existing_returns200() throws Exception {
        when(userService.findById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getUser_missing_returns404() throws Exception {
        when(userService.findById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    @WithMockUser
    void createUser_validRequest_returns201WithLocation() throws Exception {
        when(userService.create(any())).thenReturn(sampleResponse());

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("jdoe");
        request.setEmail("jdoe@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/users/1")));
    }

    @Test
    @WithMockUser
    void createUser_blankUsername_returns400WithFieldError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("");          // violates @NotBlank
        request.setEmail("x@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'username')]").exists());
    }
}
