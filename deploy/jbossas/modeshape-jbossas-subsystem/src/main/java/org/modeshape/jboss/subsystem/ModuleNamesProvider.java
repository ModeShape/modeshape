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
package org.modeshape.jboss.subsystem;

import java.io.IOException;
import java.util.Properties;
import org.jboss.logging.Logger;
import org.modeshape.common.util.StringUtil;

/**
 * Class which provides other AS services module names for built-in ModeShape components like sequencers, index-providers etc 
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class ModuleNamesProvider {

    private static final Logger LOG = Logger.getLogger(ModuleNamesProvider.class.getPackage().getName());
    private static final Properties MODULE_NAME_BY_COMPONENT_FQN;
    
    static {
        try {
            MODULE_NAME_BY_COMPONENT_FQN = new Properties();
            MODULE_NAME_BY_COMPONENT_FQN.load(ModuleNamesProvider.class.getResourceAsStream("modules.properties"));
            if (MODULE_NAME_BY_COMPONENT_FQN.isEmpty()) {
                throw new IllegalArgumentException("Cannot load the module names mapping file");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private ModuleNamesProvider() {
    }
    
    protected static String moduleNameFor(String componentFQN) {
        String moduleName = MODULE_NAME_BY_COMPONENT_FQN.getProperty(componentFQN);
        if (StringUtil.isBlank(moduleName)) {
            int index = componentFQN.lastIndexOf(".");
            moduleName = index != -1 ? componentFQN.substring(0, index) : componentFQN;
        } 
        LOG.debugv("Module name for {0} has been resolved to {1}", componentFQN, moduleName);
        return moduleName;
    }
}
