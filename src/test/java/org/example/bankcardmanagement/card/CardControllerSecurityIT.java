package org.example.bankcardmanagement.card;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bankcardmanagement.card.domain.Card;
import org.example.bankcardmanagement.card.domain.CardStatus;
import org.example.bankcardmanagement.card.repository.CardRepository;
import org.example.bankcardmanagement.security.api.dto.AuthLoginRequest;
import org.example.bankcardmanagement.security.api.dto.AuthRegisterRequest;
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

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CardControllerSecurityIT {

    private static final String JWT_SECRET_BASE64 = randomBase64(32);

    @DynamicPropertySource
    static void jwtProps(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.secret", () -> JWT_SECRET_BASE64);
        registry.add("app.security.jwt.access-token-ttl-seconds", () -> 900);
    }

    @Autowired
    WebApplicationContext webApplicationContext;

    MockMvc mockMvc;

    ObjectMapper objectMapper;

    @Autowired
    CardRepository cardRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        cardRepository.deleteAll();
    }

    @Test
    void userSeesOnlyOwnCards() throws Exception {
        String user1 = unique("user1");
        String user2 = unique("user2");

        register(user1, "StrongPass123");
        register(user2, "StrongPass123");

        cardRepository.save(card(user1));
        cardRepository.save(card(user2));

        String token = loginAndGetToken(user1, "StrongPass123");

        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].owner").value(user1));
    }

    @Test
    void adminCanSearchByOwner() throws Exception {
        String user1 = unique("user1");
        register(user1, "StrongPass123");
        cardRepository.save(card(user1));
        cardRepository.save(card("someone"));

        String admin = unique("admin");
        createAdmin(admin, "AdminStrongPass123");
        String token = loginAndGetToken(admin, "AdminStrongPass123");

        mockMvc.perform(get("/api/cards")
                        .queryParam("owner", user1)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].owner").value(user1));
    }

    private String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRegisterRequest(username, password))))
                .andExpect(status().isCreated());
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

    private void createAdmin(String username, String password) {
        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleInitializer.ROLE_ADMIN)
                .orElseThrow();

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        user.setRoles(Set.of(adminRole));
        appUserRepository.save(user);
    }

    private Card card(String owner) {
        AppUser ownerUser = appUserRepository.findByUsernameIgnoreCase(owner)
                .orElseGet(() -> {
                    AppUser u = new AppUser();
                    u.setUsername(owner);
                    u.setPasswordHash("dummy");
                    u.setEnabled(true);
                    u.setCreatedAt(Instant.now());
                    return appUserRepository.save(u);
                });

        Card card = new Card();
        card.setNumberEncrypted("dummy");
        card.setNumberLast4("0000");
        card.setOwnerUser(ownerUser);
        card.setExpiryDate(LocalDate.now().plusYears(1));
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.valueOf(100));
        return card;
    }

    private static String randomBase64(int bytesLen) {
        byte[] bytes = new byte[bytesLen];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
