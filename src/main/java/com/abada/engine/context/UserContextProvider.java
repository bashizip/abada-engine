package com.abada.engine.context;

import java.util.List;

public interface UserContextProvider {

    /**
     * Returns the currently authenticated username.
     */
    String getUsername();

    /**
     * Returns the groups the current user belongs to.
     */
    List<String> getGroups();
}
