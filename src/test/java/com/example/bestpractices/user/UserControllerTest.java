package com.example.bestpractices.user;

import com.example.bestpractices.user.dto.CreateUserRequest;
import com.example.bestpractices.user.dto.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Best practices demonstrated:
 * - @WebMvcTest is a slice test: only the web layer is loaded (fast, no DB or full context)
 * - @MockBean replaces the real UserService with a Mockito mock in the Spring context
 * - @Import(SecurityConfig.class) tests with the real security rules; use @WithMockUser for auth
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

    @MockBean
    private UserService userService;

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
