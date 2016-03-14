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

import java.util.Set;
import org.modeshape.jcr.ModeShapeEngine.State;
import org.modeshape.shell.ShellI18n;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.cmd.ShellCommand;

/**
 *
 * @author kulikov
 */
public class ShowCommand extends ShellCommand {

    public ShowCommand() {
        super("show");
        addChild(new ShowRepositories());
        addChild(new ShowState());
    }

    private class ShowRepositories extends ShellCommand {

        public ShowRepositories() {
            super("repositories", ShellI18n.engineShowRepositoriesHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            Set<String> names = session.engine().getRepositoryNames();
            if (names.isEmpty()) {
                return "<empty-set>";
            }
            
            StringBuilder builder = new StringBuilder();
            for (String s : names) {
                builder.append(s);
                builder.append("\n");
            }
            return builder.toString();
        }

    }
    
    private class ShowState extends ShellCommand {

        public ShowState() {
            super("state", ShellI18n.engineShowRepositoryStateHelp);
        }

        @Override
        public String exec(ShellSession session) throws Exception {
            String name = this.optionValue("--repository-name");
            
            if (name == null) {
                throw new IllegalArgumentException();
            }
            
            State state = session.engine().getRepositoryState(name);
            if (state == null) {
                throw new IllegalStateException("Unknown repository name " + name);
            }
            
            return state.toString();
        }

    }
    
}
