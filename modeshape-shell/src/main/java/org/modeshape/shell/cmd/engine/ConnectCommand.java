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
package org.modeshape.shell.cmd.engine;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.ShellI18n;
import org.modeshape.shell.cmd.ShellCommand;

/**
 *
 * @author kulikov
 */
public class ConnectCommand extends ShellCommand {

    private final static String RESPONSE  = "Connected to workspace '%s' as user '%s'";

    public ConnectCommand() {
        super("connect", ShellI18n.connectHelp);
    }

    @Override
    public String exec(ShellSession session) throws Exception {
        String repoName = args(0);
        if (repoName == null) {
            throw new IllegalArgumentException(help());
        }
        if (session.isAlive()) {
            return "Session already exists";
        }
        
        String userName = optionValue("--user");
        String password = optionValue("--password");
        String workspace = optionValue("--workspace");

        session.setPath("/");
        session.setRepository(session.engine().getRepository(repoName));
        if (userName == null && password == null && workspace == null) {
            session.setJcrSession(session.getRepository().login());
            session.setInterpreter(ShellSession.JCR_SESSION_INTERPRETER);
            return String.format(RESPONSE, "default", "anonymous");
        }

        if (userName == null && password == null && workspace != null) {
            session.setJcrSession(session.getRepository().login(workspace));
            session.setInterpreter(ShellSession.JCR_SESSION_INTERPRETER);
            return String.format(RESPONSE, workspace, "anonymous");
        }

        char[] paswd = password != null ? password.toCharArray() : new char[]{};
        Credentials creds = new SimpleCredentials(userName, paswd);
        if (workspace != null) {
            session.setJcrSession(session.getRepository().login(creds, workspace));
            session.setInterpreter(ShellSession.JCR_SESSION_INTERPRETER);
            return String.format(RESPONSE, workspace, userName);
        }
        
        session.setJcrSession(session.getRepository().login(creds));
        session.setInterpreter(ShellSession.JCR_SESSION_INTERPRETER);
        return String.format(RESPONSE, "default", userName);
    }

}
