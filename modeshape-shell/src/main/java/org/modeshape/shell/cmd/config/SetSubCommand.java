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
import org.modeshape.schematic.DocumentFactory;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.shell.ShellSession;

/**
 *
 * @author kulikov
 */
public class SetSubCommand extends SubCommand {

    public SetSubCommand(SubCommand parent, String name, String type, String defaultValue, String help) {
        super(parent, name, type, defaultValue, help);
    }

    @Override
    public String exec(ShellSession session) throws Exception {
        String value = args(0);
        if (value == null) {
            return help();
        }
        
        RepositoryConfiguration cfg = session.getRepositoryConfiguration();
        
        List<String> segments = path();
        EditableDocument newDoc = cfg.edit();
        EditableDocument doc = findOrCreateEnclosingDocument(newDoc, segments);
        
        if (type.equals("array")) {
            String[] tokens = value.split(",");
            doc.setArray(segments.get(segments.size() - 1), (Object[]) tokens);
        } else {        
            doc.set(segments.get(segments.size() - 1), value);
        }
        
        RepositoryConfiguration cfg1 = new RepositoryConfiguration(newDoc, cfg.getName());
        
        session.setRepositoryConfiguration(cfg1);
        return "";
    }

    private EditableDocument findOrCreateEnclosingDocument(EditableDocument config, List<String> path) {
        EditableDocument doc = config;
        for (int i = 0; i < path.size() - 1; i++) {
            Object obj = doc.getDocument(path.get(i));
            if (obj == null) {
                obj = DocumentFactory.newDocument();
                doc.set(path.get(i), obj);
            }
            doc = ((Document) obj).editable();
        }
        return doc;
    }
    
    @Override
    protected SubCommand create(SubCommand parent, String name, String type, String defaultValue, String help) {
        return new SetSubCommand(parent, name, type, defaultValue, help);
    }
}
