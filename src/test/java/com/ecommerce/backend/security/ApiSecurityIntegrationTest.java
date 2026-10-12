package com.ecommerce.backend.security;

import com.ecommerce.backend.service.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void securesCustomerAndAdminEndpointsAndEnforcesOwnership() throws Exception {
        JsonNode firstUser = register("first-customer@example.com", "customer-password", "ADMIN");
        JsonNode secondUser = register("second-customer@example.com", "another-password", "USER");
        String firstToken = firstUser.get("accessToken").asText();
        String adminToken = jwtTokenService.createToken(999L, "ADMIN");
        Long firstUserId = firstUser.get("id").asLong();
        Long secondUserId = secondUser.get("id").asLong();

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/cart/{userId}", firstUserId)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/user/{userId}", firstUserId)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/cart/{userId}", secondUserId)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/user/{userId}", secondUserId)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/cart/user/{userId}", secondUserId)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isForbidden());

        String product = """
                {"name":"Secured test product","description":"Test","price":12.5,"quantity":4,"category":"Test"}
                """;
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(product))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(product))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(product))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/orders/999999/status")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + secondUserId + ",\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + secondUserId + "}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/api/auth/register"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/api/products"))
                .andExpect(jsonPath("$").isArray());
    }

    private JsonNode register(String email, String password, String role) throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterPayload(
                "Test Customer", email, password, role));
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record RegisterPayload(String name, String email, String password, String role) {
    }
}
