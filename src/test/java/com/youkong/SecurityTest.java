package com.youkong;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkong.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    private static final String LOGIN_BODY =
            "{\"username\":\"owner\",\"password\":\"youkong123\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountService accountService;

    @BeforeEach
    void ensureAccount() {
        accountService.createDefaultIfAbsent(1L, "owner", "youkong123");
    }

    @Test
    @DisplayName("未登录访问后台接口返回 401")
    void adminRequiresToken() throws Exception {
        mockMvc.perform(get("/api/admin/shops/1/staff"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    @DisplayName("登录成功返回 token，带上后可访问后台接口")
    void loginThenAccessAdmin() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/admin/shops/1/staff").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("密码错误返回 4002，不返回 token")
    void wrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"owner\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("伪造的 token 会被拒绝")
    void invalidTokenRejected() throws Exception {
        mockMvc.perform(get("/api/admin/shops/1/staff")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("顾客端接口不需要登录")
    void customerApiIsPublic() throws Exception {
        mockMvc.perform(get("/api/availability")
                        .param("staffId", "1")
                        .param("serviceId", "1")
                        .param("date", LocalDate.now().plusDays(9).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
    }
}
