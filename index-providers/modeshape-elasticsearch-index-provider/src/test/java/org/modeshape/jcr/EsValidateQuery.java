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
package org.modeshape.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.query.Row;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author kulikov
 */
public class EsValidateQuery {
    
    public static ValidateQuery.ValidationBuilder validateQuery() {
        return new ValidationBuilder();
    }
    
    public static class ValidationBuilder extends org.modeshape.jcr.ValidateQuery.Builder {
        
        @Override
        public ValidateQuery.ValidationBuilder hasNodesAtPaths( String... paths ) {
            return setValidator(new NodesValidator(paths));
        }
    }
    
    protected static class NodesValidator implements ValidateQuery.Validator {
        private final String[] path;

        protected NodesValidator( String[] path ) {
            this.path = path;
        }

        @Override
        public void checkRow( Row row,
                              String[] selectorNames ) throws RepositoryException {
            boolean found = false;
            for (int i = 0; i < path.length; i++) {
                if (row.getPath().equals(path[i])) {
                    found = true;
                    break;
                }
            }
            assertTrue("Path " + row.getPath() + " not found", found);
        }
    }
    
}
