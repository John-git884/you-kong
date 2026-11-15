package com.youkong;

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

/**
 * 接口层冒烟测试：不依赖真实端口，走完整的 Spring MVC 链路。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("店铺详情接口返回示例店铺")
    void shopDetail() throws Exception {
        mockMvc.perform(get("/api/shops/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("拾光美甲 · 示例店"));
    }

    @Test
    @DisplayName("可用时段接口：无预约时返回该员工当天可选开始时间")
    void availability() throws Exception {
        String date = LocalDate.now().plusDays(5).toString();
        mockMvc.perform(get("/api/availability")
                        .param("staffId", "2")
                        .param("serviceId", "1")
                        .param("date", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.slots").isNotEmpty());
    }

    @Test
    @DisplayName("创建预约 -> 同档期再建一次应返回 2001 档期已被占用")
    void createThenConflict() throws Exception {
        String startAt = LocalDate.now().plusDays(2) + "T14:00:00";
        String body = """
                {
                  "staffId": 2,
                  "serviceId": 3,
                  "startAt": "%s",
                  "customerName": "测试顾客",
                  "customerPhone": "13900139000"
                }
                """.formatted(startAt);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001));
    }

    @Test
    @DisplayName("非 15 分钟整倍数的开始时间应被拒绝")
    void rejectUnalignedStartTime() throws Exception {
        String startAt = LocalDate.now().plusDays(3) + "T14:07:00";
        String body = """
                {
                  "staffId": 1,
                  "serviceId": 1,
                  "startAt": "%s",
                  "customerName": "测试顾客",
                  "customerPhone": "13900139000"
                }
                """.formatted(startAt);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }
}
