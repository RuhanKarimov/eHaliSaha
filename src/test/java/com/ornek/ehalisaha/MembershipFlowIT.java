package com.ornek.ehalisaha;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcWebDriverAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(classes = com.ornek.ehalisaha.ehalisahabackend.EHalisahaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ImportAutoConfiguration(exclude = MockMvcWebDriverAutoConfiguration.class)

class MembershipFlowIT {

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
    void membershipRequest_shouldBeVisibleInStatus_andDuplicateShouldConflict() throws Exception {
        // owner creates facility
        JsonNode fac = postJson("/api/owner/facilities", "owner1", "owner123", "{\"name\":\"Arena Membership IT\",\"address\":\"Merkez\"}");
        long facilityId = fac.get("id").asLong();

        // member status before request: no membership, no request
        mvc.perform(get("/api/member/membership-status")
                        .with(httpBasic("member1","member123"))
                        .param("facilityId", String.valueOf(facilityId)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.member").value(false));

        // member sends membership request
        String reqBody = "{\"facilityId\":" + facilityId + "}";
        mvc.perform(post("/api/member/membership-requests")
                        .with(httpBasic("member1","member123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // status after request: requestStatus=PENDING
        mvc.perform(get("/api/member/membership-status")
                        .with(httpBasic("member1","member123"))
                        .param("facilityId", String.valueOf(facilityId)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.member").value(false))
                .andExpect(jsonPath("$.requestStatus").value("PENDING"));

        // duplicate request -> 409
        mvc.perform(post("/api/member/membership-requests")
                        .with(httpBasic("member1","member123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isConflict());

        // owner approves
        String listJson = mvc.perform(get("/api/owner/membership-requests")
                        .with(httpBasic("owner1","owner123")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        long requestId = om.readTree(listJson).get(0).get("id").asLong();

        mvc.perform(post("/api/owner/membership-requests/" + requestId + "/approve")
                        .with(httpBasic("owner1","owner123")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // member status after approve: member=true, membershipStatus=ACTIVE
        mvc.perform(get("/api/member/membership-status")
                        .with(httpBasic("member1","member123"))
                        .param("facilityId", String.valueOf(facilityId)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.member").value(true))
                .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.requestStatus").value("APPROVED"));
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
