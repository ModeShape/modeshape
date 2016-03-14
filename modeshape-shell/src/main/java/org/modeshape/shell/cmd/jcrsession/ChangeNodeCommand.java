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
package org.modeshape.shell.cmd.jcrsession;

import org.modeshape.shell.cmd.ShellCommand;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.ShellI18n;

/**
 *
 * @author kulikov
 */
public class ChangeNodeCommand extends ShellCommand {

    public ChangeNodeCommand() {
        super("cd", ShellI18n.changeNodeHelp);
    }

    @Override
    public String exec(ShellSession session) throws Exception {
        //required argument
        String dest = args(0);

        //validate arguments
        if (!allArgsSpecified(dest)) {
            return help();
        }

        //if path is absolute start from root node otherwise from current position
        String startPoint = isAbsolute(dest) ? "/" : session.getPath();
        String[] segments = dest.split("/");

        try {
            String path = changePathStepByStep(session, startPoint, segments);
            session.setPath(path);
            return SILENCE;
        } catch (PathNotFoundException e) {
            return "Unknown node in the path: " + e.getMessage();
        }
    }

    private String changePathStepByStep(ShellSession session, String from, String[] segments) throws PathNotFoundException, RepositoryException {
        String path = from;
        for (String name : segments) {
            path = changePathOnOneStep(session, path, name);
        }
        return path;
    }

    private String changePathOnOneStep(ShellSession s, String from, String step)
            throws PathNotFoundException, RepositoryException {
        Session session = s.jcrSession();
        boolean stepUp = step.equals("..");

        String path = from;
        Node node = session.getNode(path);
        if (stepUp) {
            path = node.getParent().getPath();
        } else {
            path = node.getNode(step).getPath();
        }

        return path;
    }

    private boolean isAbsolute(String path) {
        return path.startsWith("/");
    }
}
