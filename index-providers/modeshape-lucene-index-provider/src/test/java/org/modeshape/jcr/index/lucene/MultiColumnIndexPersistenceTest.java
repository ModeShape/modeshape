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
package org.modeshape.jcr.index.lucene;

import static org.junit.Assert.assertEquals;
import java.util.UUID;
import org.junit.Test;
import org.modeshape.jcr.value.PropertyType;

/**
 * Tests CRUD operations on the {@link MultiColumnIndex} index.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class MultiColumnIndexPersistenceTest extends SingleColumnIndexPersistenceTest {

    @Override
    protected LuceneIndex createIndex( String name ) {
        return new MultiColumnIndex(name + "-multi-valued", "default", config, PropertiesTestUtil.ALLOWED_PROPERTIES, context);
    }

    @Test
    public void shouldUpdateMultipleValuesForSameNodeWithIndividualCommits() throws Exception {
        String nodeKey = UUID.randomUUID().toString();
        //call 'add' a number of times, committing each time
        addMultiplePropertiesToSameNode(index, nodeKey, 2, PropertyType.LONG);
        index.commit();
        addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.DECIMAL);
        index.commit();
        addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.STRING);
        index.commit();
        addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.STRING);
        index.commit();
        addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.URI);
        index.commit();
        assertEquals(1, index.estimateTotalCount());
    }

    @Test
    public void shouldUpdateMultipleValuesForSameNodeWithBatchCommit() throws Exception {
        String nodeKey = UUID.randomUUID().toString();
        //call 'add' without committing  
        addMultiplePropertiesToSameNode(index, nodeKey, 2, PropertyType.LONG);
        addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.DECIMAL);
        addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.STRING);
        addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.STRING);
        addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.URI);

        index.commit();
        assertEquals(1, index.estimateTotalCount());
    }
    
    @Test
    @Override
    public void shouldRemoveAllValues() throws Exception {
        String nodeKey = UUID.randomUUID().toString();
        addMultiplePropertiesToSameNode(index, nodeKey, 2, PropertyType.LONG);
        addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.STRING);
        index.commit();

        index.remove(nodeKey);
        index.commit();
        assertEquals(0, index.estimateTotalCount());
    }
    
    @Test
    public void shouldRemoveSingleValue() throws Exception {
        String nodeKey = UUID.randomUUID().toString();
        addMultiplePropertiesToSameNode(index, nodeKey, 2, PropertyType.LONG);
        String stringProperty = addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.STRING);
        index.commit();
        
        index.remove(nodeKey, stringProperty);
        index.commit();
        assertEquals(1, index.estimateTotalCount());
    }
}
