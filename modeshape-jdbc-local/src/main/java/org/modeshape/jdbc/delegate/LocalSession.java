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
package org.modeshape.jdbc.delegate;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**

 */
public class LocalSession {

    private static LocalSession instance = new LocalSession();

    public static LocalSession getLocalSessionInstance() {
        return instance;
    }

    private ThreadLocal<LocalSession> tlocal = new ThreadLocal<LocalSession>() {
        @Override
        protected LocalSession initialValue() {
            LocalSession ls = new LocalSession();
            LocalRepositoryDelegate.TRANSACTION_IDS.add(ls);
            return ls;
        }
    };

    private Session session;

    private void setSession( Session localSession ) {
        session = localSession;
    }

    public LocalSession getLocalSession() {
        return tlocal.get();
    }

    public LocalSession getLocalSession( final Repository repository,
                                         final ConnectionInfo connInfo )
        throws LoginException, NoSuchWorkspaceException, RepositoryException {
        LocalSession lsession = tlocal.get();

        Credentials credentials = connInfo.getCredentials();
        String workspaceName = connInfo.getWorkspaceName();
        Session session = null;
        if (workspaceName != null) {
            session = credentials != null ? repository.login(credentials, workspaceName) : repository.login(workspaceName);
        } else {
            session = credentials != null ? repository.login(credentials) : repository.login();
        }
        // this shouldn't happen, but in testing it did occur only because of
        // the repository not being setup correctly
        assert session != null;

        lsession.setSession(session);

        return lsession;
    }

    public Session getSession() {
        return session;
    }

    public void remove() {
        tlocal.remove();
        LocalRepositoryDelegate.TRANSACTION_IDS.remove(this);
        session.logout();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Session:");
        sb.append(session.toString());
        return sb.toString();
    }

}
