package org.example.bankcardmanagement.transfer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bankcardmanagement.card.domain.Card;
import org.example.bankcardmanagement.card.domain.CardStatus;
import org.example.bankcardmanagement.card.repository.CardRepository;
import org.example.bankcardmanagement.security.api.dto.AuthLoginRequest;
import org.example.bankcardmanagement.security.api.dto.AuthRegisterRequest;
import org.example.bankcardmanagement.security.domain.AppUser;
import org.example.bankcardmanagement.security.repository.AppUserRepository;
import org.example.bankcardmanagement.transfer.api.dto.TransferRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class TransferControllerIT {

    private static final String JWT_SECRET_BASE64 = randomBase64(32);

    @DynamicPropertySource
    static void jwtProps(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.secret", () -> JWT_SECRET_BASE64);
        registry.add("app.security.jwt.access-token-ttl-seconds", () -> 900);
    }

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    CardRepository cardRepository;

    @Autowired
    AppUserRepository appUserRepository;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        cardRepository.deleteAll();
    }

    @Test
    void transferBetweenOwnCards_success() throws Exception {
        String user = unique("user");
        register(user, "StrongPass123");
        String token = loginAndGetToken(user, "StrongPass123");

        Card from = cardRepository.save(card(user, BigDecimal.valueOf(100)));
        Card to = cardRepository.save(card(user, BigDecimal.valueOf(10)));

        TransferRequestDto req = new TransferRequestDto(from.getId(), to.getId(), BigDecimal.valueOf(30));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fromCardId").value(from.getId().toString()))
                .andExpect(jsonPath("$.toCardId").value(to.getId().toString()))
                .andExpect(jsonPath("$.amount").value(30))
                .andExpect(jsonPath("$.createdBy").value(user))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        Card fromAfter = cardRepository.findById(from.getId()).orElseThrow();
        Card toAfter = cardRepository.findById(to.getId()).orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals(0, fromAfter.getBalance().compareTo(BigDecimal.valueOf(70)));
        org.junit.jupiter.api.Assertions.assertEquals(0, toAfter.getBalance().compareTo(BigDecimal.valueOf(40)));
    }

    @Test
    void transferBetweenDifferentOwners_rejected() throws Exception {
        String user = unique("user");
        register(user, "StrongPass123");
        String token = loginAndGetToken(user, "StrongPass123");

        Card from = cardRepository.save(card(user, BigDecimal.valueOf(100)));
        Card to = cardRepository.save(card("someone_else", BigDecimal.valueOf(10)));

        TransferRequestDto req = new TransferRequestDto(from.getId(), to.getId(), BigDecimal.valueOf(30));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
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

    private Card card(String owner, BigDecimal balance) {
        AppUser ownerUser = appUserRepository.findByUsernameIgnoreCase(owner)
                .orElseGet(() -> {
                    AppUser u = new AppUser();
                    u.setUsername(owner);
                    u.setPasswordHash("dummy");
                    u.setEnabled(true);
                    u.setCreatedAt(java.time.Instant.now());
                    return appUserRepository.save(u);
                });

        Card card = new Card();
        card.setNumberEncrypted("dummy");
        card.setNumberLast4("0000");
        card.setOwnerUser(ownerUser);
        card.setExpiryDate(LocalDate.now().plusYears(1));
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(balance);
        return card;
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
