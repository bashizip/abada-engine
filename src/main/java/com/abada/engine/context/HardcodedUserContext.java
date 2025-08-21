package com.abada.engine.context;


import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A simple fixed user context for local development or testing.
 * Replace it with a dynamic implementation in production.
 */
@Component
public class HardcodedUserContext implements UserContextProvider {

    @Override
    public String getUsername() {
        return "alice"; // Simulated logged-in user
    }

    @Override
    public List<String> getGroups() {
        return List.of("finance", "qa");
    }
}
