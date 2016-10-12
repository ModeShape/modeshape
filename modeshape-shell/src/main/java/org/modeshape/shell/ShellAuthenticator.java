/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.shell;

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

/**
 * Authenticator that uses user name and password for the authentication.
 * 
 * @author kulikov
 */
public class ShellAuthenticator implements PasswordAuthenticator {
    private final String user;
    private final String password;
   
    /**
     * Creates new authenticator for the given user.
     * 
     * @param user user's name
     * @param password user's password.
     */
    public ShellAuthenticator(String user, String password) {
        this.user = user;
        this.password = password;
    }
    
    @Override
    public boolean authenticate(String user, String password, ServerSession session) {
        return this.user.equals(user) && this.password.equals(password);
    }
    
}
