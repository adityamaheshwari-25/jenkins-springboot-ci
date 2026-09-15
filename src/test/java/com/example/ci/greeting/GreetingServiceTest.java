package com.example.ci.greeting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GreetingServiceTest {
    private final GreetingService service = new GreetingService();

    @Test
    void greetsTheProvidedName() {
        Greeting result = service.greet("Aditya");

        assertThat(result.message()).isEqualTo("Hello, Aditya!");
        assertThat(result.id()).isEqualTo(1);
    }

    @Test
    void usesDefaultNameWhenNameIsBlank() {
        Greeting result = service.greet(" ");

        assertThat(result.message()).isEqualTo("Hello, Jenkins learner!");
    }
}
