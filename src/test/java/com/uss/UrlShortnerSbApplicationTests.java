package com.uss;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "OTP_MAIL_FROM=no-reply@test.invalid",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test",
        "SMTP_HOST=localhost"
})
class UrlShortnerSbApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void protectedProfileReturnsJsonUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void registrationCannotCompleteWithoutTheVerifiedOneTimeToken() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/public/complete-registration")
                        .contentType("application/json")
                        .content("""
                                {"email":"new-person@example.com","username":"new_person","password":"password123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.registrationToken").exists());
    }

    @Test
    void obsoleteDirectRegistrationEndpointIsNotMapped() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/public/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"new-person@example.com","username":"new_person","password":"password123"}
                                """))
                .andExpect(status().isNotFound());
    }
}
