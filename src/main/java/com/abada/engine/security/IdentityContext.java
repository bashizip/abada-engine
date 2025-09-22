package com.abada.engine.security;

import java.util.Optional;

/**
 * A ThreadLocal-based context holder for the current request's {@link Identity}.
 * This allows any component in the application to access the identity of the user
 * who initiated the current request processing thread.
 */
public final class IdentityContext {

    private static final ThreadLocal<Identity> CONTEXT = new ThreadLocal<>();

    private IdentityContext() {}

    public static void set(Identity identity) {
        CONTEXT.set(identity);
    }

    public static Optional<Identity> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}