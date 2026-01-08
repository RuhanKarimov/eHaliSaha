package com.ornek.ehalisaha;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = com.ornek.ehalisaha.ehalisahabackend.EHalisahaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ReservationFlowIT {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("ehalisaha")
            .withUsername("ehalisaha")
            .withPassword("ehalisaha");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.flyway.enabled", () -> true);
    }

    @Autowired MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();


    @Test
    void reservationOverlapShouldReturnConflict() throws Exception {
        // 1) Owner creates facility
        String facBody = """
            {"name":"Arena IT","address":"Merkez"}
        """;

        JsonNode fac = postJson("/api/owner/facilities", "owner1", "owner123", facBody);
        long facilityId = fac.get("id").asLong();

        // 2) Owner creates pitch (body facilityId zorunlu)
        String pitchBody = """
            {"name":"Saha A","facilityId": %d}
        """.formatted(facilityId);

        JsonNode pitch = postJson("/api/owner/facilities/" + facilityId + "/pitches", "owner1", "owner123", pitchBody);
        long pitchId = pitch.get("id").asLong();

        // 3) Owner sets base pricing (60dk)
        String pricingBody = """
            {"pitchId": %d, "durationMinutes": 60, "price": 100}
        """.formatted(pitchId);
        postJson("/api/owner/pricing", "owner1", "owner123", pricingBody);

        // 4) Member sends membership request
        String reqBody = """
            {"facilityId": %d}
        """.formatted(facilityId);
        postJson("/api/member/membership-requests", "member1", "member123", reqBody);

        // 5) Owner lists pending requests and approves
        String listJson = mvc.perform(get("/api/owner/membership-requests")
                        .with(httpBasic("owner1","owner123")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        JsonNode arr = om.readTree(listJson);
        long requestId = arr.get(0).get("id").asLong();

        mvc.perform(post("/api/owner/membership-requests/" + requestId + "/approve")
                        .with(httpBasic("owner1","owner123")))
                .andExpect(status().is2xxSuccessful());

        // 6) Member makes SAME reservation twice => 2nd should be 409 (overlap)
        String resBody = """
        {
          "pitchId": %d,
          "startTime": "2030-01-01T10:00:00Z",
          "durationMinutes": 60,
          "paymentMethod": "CARD",
          "players": [{"fullName":"Ali Veli","jerseyNo":10}]
        }
        """.formatted(pitchId);

        mvc.perform(post("/api/member/reservations")
                        .with(httpBasic("member1","member123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resBody))
                .andExpect(status().is2xxSuccessful());

        mvc.perform(post("/api/member/reservations")
                        .with(httpBasic("member1","member123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resBody))
                .andExpect(status().isConflict());
    }

    private JsonNode postJson(String url, String user, String pass, String body) throws Exception {
        String json = mvc.perform(post(url)
                        .with(httpBasic(user, pass))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(json);
    }
}
