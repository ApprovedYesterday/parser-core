package com.tracker.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.application.port.out.ScraperPort;
import com.tracker.domain.Money;
import com.tracker.domain.PricePoint;
import com.tracker.domain.Sku;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("tracker")
        .withUsername("tracker")
        .withPassword("tracker");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScraperPort scraperPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldTrackProduct() throws Exception {
        when(scraperPort.fetchPrice(any(Sku.class)))
            .thenReturn(new PricePoint(
                new Sku("TEST-SKU-001"),
                new Money(new BigDecimal("1499.00"), "RUB"),
                Instant.now()
            ));

        mockMvc.perform(post("/api/v1/track")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", "TEST-SKU-001"))))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnEmptyHistoryWhenNoData() throws Exception {
        mockMvc.perform(get("/api/v1/products/NONEXISTENT/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnHistoryWithResolution() throws Exception {
        when(scraperPort.fetchPrice(any(Sku.class)))
            .thenReturn(new PricePoint(
                new Sku("TEST-SKU-002"),
                new Money(new BigDecimal("999.00"), "RUB"),
                Instant.now()
            ));

        mockMvc.perform(post("/api/v1/track")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", "TEST-SKU-002"))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/TEST-SKU-002/history")
                .param("resolution", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].sku").value("TEST-SKU-002"))
            .andExpect(jsonPath("$[0].price").value(999.00));
    }
}
