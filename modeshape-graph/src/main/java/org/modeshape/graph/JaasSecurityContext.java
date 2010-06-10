package org.modeshape.graph;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.Reflection;

/**
 * JAAS-based {@link SecurityContext security context} that provides authentication and authorization through the JAAS
 * {@link LoginContext login context}.
 */
@NotThreadSafe
public final class JaasSecurityContext implements SecurityContext {

    private static final Logger LOGGER = Logger.getLogger(JaasSecurityContext.class);

    private final LoginContext loginContext;
    private final String userName;
    private final Set<String> entitlements;
    private boolean loggedIn;

    /**
     * Create a {@link JaasSecurityContext} with the supplied {@link Configuration#getAppConfigurationEntry(String) application
     * configuration name}.
     * 
     * @param realmName the name of the {@link Configuration#getAppConfigurationEntry(String) JAAS application configuration name}
     *        ; may not be null
     * @throws IllegalArgumentException if the <code>name</code> is null
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), or if the
     *         default callback handler JAAS property was not set or could not be loaded
     */
    public JaasSecurityContext( String realmName ) throws LoginException {
        this(new LoginContext(realmName));
    }

    /**
     * Create a {@link JaasSecurityContext} with the supplied {@link Configuration#getAppConfigurationEntry(String) application
     * configuration name} and a {@link Subject JAAS subject}.
     * 
     * @param realmName the name of the {@link Configuration#getAppConfigurationEntry(String) JAAS application configuration name}
     * @param subject the subject to authenticate
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), if the default
     *         callback handler JAAS property was not set or could not be loaded, or if the <code>subject</code> is null or
     *         unknown
     */
    public JaasSecurityContext( String realmName,
                                Subject subject ) throws LoginException {
        this(new LoginContext(realmName, subject));
    }

    /**
     * Create a {@link JaasSecurityContext} with the supplied {@link Configuration#getAppConfigurationEntry(String) application
     * configuration name} and a {@link CallbackHandler JAAS callback handler} to create a new {@link JaasSecurityContext JAAS
     * login context} with the given user ID and password.
     * 
     * @param realmName the name of the {@link Configuration#getAppConfigurationEntry(String) JAAS application configuration name}
     * @param userId the user ID to use for authentication
     * @param password the password to use for authentication
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), or if the
     *         <code>callbackHandler</code> is null
     */

    public JaasSecurityContext( String realmName,
                                String userId,
                                char[] password ) throws LoginException {
        this(new LoginContext(realmName, new UserPasswordCallbackHandler(userId, password)));
    }

    /**
     * Create a {@link JaasSecurityContext} with the supplied {@link Configuration#getAppConfigurationEntry(String) application
     * configuration name} and the given callback handler.
     * 
     * @param realmName the name of the {@link Configuration#getAppConfigurationEntry(String) JAAS application configuration name}
     *        ; may not be null
     * @param callbackHandler the callback handler to use during the login process; may not be null
     * @throws LoginException if there <code>name</code> is invalid (or there is no login context named "other"), or if the
     *         <code>callbackHandler</code> is null
     */

    public JaasSecurityContext( String realmName,
                                CallbackHandler callbackHandler ) throws LoginException {
        this(new LoginContext(realmName, callbackHandler));
    }

    /**
     * Creates a new JAAS security context based on the given login context. If {@link LoginContext#login() login} has not already
     * been invoked on the login context, this constructor will attempt to invoke it.
     * 
     * @param loginContext the login context to use; may not be null
     * @throws LoginException if the context has not already had {@link LoginContext#login() its login method} invoked and an
     *         error occurs attempting to invoke the login method.
     * @see LoginContext
     */
    public JaasSecurityContext( LoginContext loginContext ) throws LoginException {
        CheckArg.isNotNull(loginContext, "loginContext");
        this.entitlements = new HashSet<String>();
        this.loginContext = loginContext;

        if (this.loginContext.getSubject() == null) this.loginContext.login();

        this.userName = initialize(loginContext.getSubject());
        this.loggedIn = true;
    }

    /**
     * Creates a new JAAS security context based on the user name and roles from the given subject.
     * 
     * @param subject the subject to use as the provider of the user name and roles for this security context; may not be null
     */
    public JaasSecurityContext( Subject subject ) {
        CheckArg.isNotNull(subject, "subject");
        this.loginContext = null;
        this.entitlements = new HashSet<String>();
        this.userName = initialize(subject);
        this.loggedIn = true;
    }

    private String initialize( Subject subject ) {
        String userName = null;

        if (subject != null) {
            for (Principal principal : subject.getPrincipals()) {
                if (principal instanceof Group) {
                    Group group = (Group)principal;
                    Enumeration<? extends Principal> roles = group.members();

                    while (roles.hasMoreElements()) {
                        Principal role = roles.nextElement();
                        entitlements.add(role.getName());
                    }
                } else {
                    userName = principal.getName();
                    LOGGER.debug("Adding principal user name: " + userName);
                }
            }
        }

        return userName;
    }

    /**
     * {@inheritDoc SecurityContext#getUserName()}
     * 
     * @see SecurityContext#getUserName()
     */
    public String getUserName() {
        return loggedIn ? userName : null;
    }

    /**
     * {@inheritDoc SecurityContext#hasRole(String)}
     * 
     * @see SecurityContext#hasRole(String)
     */
    public boolean hasRole( String roleName ) {
        return loggedIn ? entitlements.contains(roleName) : false;
    }

    /**
     * {@inheritDoc SecurityContext#logout()}
     * 
     * @see SecurityContext#logout()
     */
    public void logout() {
        try {
            loggedIn = false;
            if (loginContext != null) loginContext.logout();
        } catch (LoginException le) {
            LOGGER.info(le, null);
        }
    }

    /**
     * A simple {@link CallbackHandler callback handler} implementation that attempts to provide a user ID and password to any
     * callbacks that it handles.
     */
    public static final class UserPasswordCallbackHandler implements CallbackHandler {

        private static final boolean LOG_TO_CONSOLE = false;

        private final String userId;
        private final char[] password;

        public UserPasswordCallbackHandler( String userId,
                                            char[] password ) {
            this.userId = userId;
            this.password = password.clone();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
         */
        public void handle( Callback[] callbacks ) throws UnsupportedCallbackException, IOException {
            boolean userSet = false;
            boolean passwordSet = false;

            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof TextOutputCallback) {

                    // display the message according to the specified type
                    if (LOG_TO_CONSOLE) {
                        continue;
                    }

                    TextOutputCallback toc = (TextOutputCallback)callbacks[i];
                    switch (toc.getMessageType()) {
                        case TextOutputCallback.INFORMATION:
                            System.out.println(toc.getMessage());
                            break;
                        case TextOutputCallback.ERROR:
                            System.out.println("ERROR: " + toc.getMessage());
                            break;
                        case TextOutputCallback.WARNING:
                            System.out.println("WARNING: " + toc.getMessage());
                            break;
                        default:
                            throw new IOException("Unsupported message type: " + toc.getMessageType());
                    }

                } else if (callbacks[i] instanceof NameCallback) {

                    // prompt the user for a username
                    NameCallback nc = (NameCallback)callbacks[i];

                    if (LOG_TO_CONSOLE) {
                        // ignore the provided defaultName
                        System.out.print(nc.getPrompt());
                        System.out.flush();
                    }

                    nc.setName(this.userId);
                    userSet = true;

                } else if (callbacks[i] instanceof PasswordCallback) {

                    // prompt the user for sensitive information
                    PasswordCallback pc = (PasswordCallback)callbacks[i];
                    if (LOG_TO_CONSOLE) {
                        System.out.print(pc.getPrompt());
                        System.out.flush();
                    }
                    pc.setPassword(this.password);
                    passwordSet = true;

                } else {
                    /*
                     * Jetty uses its own callback for setting the password.  Since we're using Jetty for integration
                     * testing of the web project(s), we have to accomodate this.  Rather than introducing a direct
                     * dependency, we'll add code to handle the case of unexpected callback handlers with a setObject method.
                     */
                    try {
                        // Assume that a callback chain will ask for the user before the password
                        if (!userSet) {
                            new Reflection(callbacks[i].getClass()).invokeSetterMethodOnTarget("object",
                                                                                               callbacks[i],
                                                                                               this.userId);
                            userSet = true;
                        } else if (!passwordSet) {
                            // Jetty also seems to eschew passing passwords as char arrays
                            new Reflection(callbacks[i].getClass()).invokeSetterMethodOnTarget("object",
                                                                                               callbacks[i],
                                                                                               new String(this.password));
                            passwordSet = true;
                        }
                        // It worked - need to continue processing the callbacks
                        continue;
                    } catch (Exception ex) {
                        // If the property cannot be set, fall through to the failure
                    }
                    throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback: "
                                                                         + callbacks[i].getClass().getName());
                }
            }

        }
    }
}
