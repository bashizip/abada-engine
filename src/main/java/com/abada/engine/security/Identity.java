package com.abada.engine.security;

import java.util.List;

/**
 * Represents the identity of the user making a request.
 * @param username The unique identifier for the user.
 * @param groups A list of groups or roles the user belongs to.
 */
public record Identity(String username, List<String> groups) {}