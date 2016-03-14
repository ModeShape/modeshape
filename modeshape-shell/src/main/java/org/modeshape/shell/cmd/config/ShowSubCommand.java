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
package org.modeshape.shell.cmd.config;

import java.util.List;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.schematic.document.Document;
import org.modeshape.shell.ShellSession;

/**
 *
 * @author kulikov
 */
public class ShowSubCommand extends SubCommand {

    public ShowSubCommand(SubCommand parent, String name, String type, String defaultValue, String help) {
        super(parent, name, type, defaultValue, help);
    }

    @Override
    public String exec(ShellSession session) throws Exception {
        RepositoryConfiguration cfg = session.getRepositoryConfiguration();
        List<String> segments = path();
        Document doc = findEnclosingDocument(cfg.edit(), segments);
                
        return doc == null ? defaultValue
                : doc.get(segments.get(segments.size() - 1)).toString();
    }
    
    private Document findEnclosingDocument(Document config, List<String> path) {
        Document doc = config;
        int i = 0;
        while ((i < path.size() - 1) && ((doc = doc.getDocument(path.get(i))) != null)) {
            i++;
        }
        return doc;
    }

    @Override
    protected SubCommand create(SubCommand parent, String name, String type, String defaultValue, String help) {
        return new ShowSubCommand(parent, name, type, defaultValue, help);
    }
    
}
