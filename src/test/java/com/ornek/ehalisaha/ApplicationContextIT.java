package com.ornek.ehalisaha;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Covered by ReservationFlowIT (Testcontainers). Kept only as example.")
@SpringBootTest(classes = com.ornek.ehalisaha.ehalisahabackend.EHalisahaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationContextIT {

    @Test
    void contextLoads() {
    }

}
