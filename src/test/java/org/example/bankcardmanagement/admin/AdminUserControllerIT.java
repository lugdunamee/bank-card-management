package org.example.bankcardmanagement.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bankcardmanagement.security.api.dto.AuthLoginRequest;
import org.example.bankcardmanagement.security.domain.AppUser;
import org.example.bankcardmanagement.security.domain.Role;
import org.example.bankcardmanagement.security.repository.AppUserRepository;
import org.example.bankcardmanagement.security.repository.RoleRepository;
import org.example.bankcardmanagement.security.service.RoleInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AdminUserControllerIT {

    private static final String JWT_SECRET_BASE64 = randomBase64(32);

    @DynamicPropertySource
    static void jwtProps(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.secret", () -> JWT_SECRET_BASE64);
        registry.add("app.security.jwt.access-token-ttl-seconds", () -> 900);
    }

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void adminEndpoint_requiresAdmin() throws Exception {
        String userToken = tokenForUser("user");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String adminToken = tokenForAdmin("admin");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String tokenForUser(String usernamePrefix) throws Exception {
        String username = unique(usernamePrefix);
        createUser(username, "StrongPass123", RoleInitializer.ROLE_USER);
        return loginAndGetToken(username, "StrongPass123");
    }

    private String tokenForAdmin(String usernamePrefix) throws Exception {
        String username = unique(usernamePrefix);
        createUser(username, "AdminStrongPass123", RoleInitializer.ROLE_ADMIN);
        return loginAndGetToken(username, "AdminStrongPass123");
    }

    private void createUser(String username, String password, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        user.setRoles(Set.of(role));
        appUserRepository.save(user);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthLoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        return json.get("accessToken").asText();
    }

    private String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String randomBase64(int bytesLen) {
        byte[] bytes = new byte[bytesLen];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
