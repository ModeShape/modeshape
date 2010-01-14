package org.modeshape.graph;

import java.util.Collections;
import java.util.Set;

/**
 * Mock security context for testing that grants a set of roles.
 */
public class MockSecurityContext implements SecurityContext {

    private final String userName;
    private final Set<String> entitlements;

    public MockSecurityContext(String userName) {
        this(userName, null);
    }

    public MockSecurityContext(String userName, Set<String> entitlements) {
        this.userName = userName;
        this.entitlements = entitlements != null ? entitlements : Collections.<String>emptySet();
    }
    
    public String getUserName() {
        return userName;
    }

    public boolean hasRole( String roleName ) {
        return entitlements.contains(roleName);
    }

    public void logout() {

    }

}
