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

import org.modeshape.jcr.JcrRepository;
import org.modeshape.shell.ShellI18n;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.cmd.ShellCommand;

/**
 *
 * @author kulikov
 */
public class ConfigureCommand extends ShellCommand {

    public ConfigureCommand() {
        super("configure", ShellI18n.engineConfigureRepositoryHelp);
    }

    @Override
    public String exec(ShellSession session) throws Exception {
        String repoName = this.args(0);
        if (repoName == null) throw new IllegalArgumentException(help());
        
        JcrRepository repository = session.engine().getRepository(repoName);
        session.setRepository(repository);
        session.setRepositoryConfiguration(repository.getConfiguration());
        session.setInterpreter(ShellSession.CONFIGURATION_INTERPRETER);
        
        return "";
    }
}
