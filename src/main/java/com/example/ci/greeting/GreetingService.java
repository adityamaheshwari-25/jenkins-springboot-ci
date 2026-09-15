package com.example.ci.greeting;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class GreetingService {
    private final AtomicLong counter = new AtomicLong();

    public Greeting greet(String name) {
        String safeName = name == null || name.isBlank() ? "Jenkins learner" : name.trim();
        return new Greeting(counter.incrementAndGet(), "Hello, " + safeName + "!");
    }
}
