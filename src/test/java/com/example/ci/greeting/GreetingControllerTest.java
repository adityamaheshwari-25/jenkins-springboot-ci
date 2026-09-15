package com.example.ci.greeting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GreetingController.class)
class GreetingControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean GreetingService greetingService;

    @Test
    void returnsGreetingAsJson() throws Exception {
        when(greetingService.greet("Jenkins")).thenReturn(new Greeting(1, "Hello, Jenkins!"));

        mockMvc.perform(get("/api/greetings").param("name", "Jenkins"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1,\"message\":\"Hello, Jenkins!\"}"));
    }
}
