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

import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.JcrQueryManagerTest;

/**
 * Extension of {@link JcrQueryManagerTest} which runs queries against Lucene indexes.
 */
@SuppressWarnings("deprecation")
public class LuceneIndexProviderQueryTest extends JcrQueryManagerTest {

    @BeforeClass
    public static void beforeAll() throws Exception {
        // Clean up the indexes and storage ...
        FileUtil.delete("target/LuceneIndexProviderQueryTest");

        String configFileName = LuceneIndexProviderQueryTest.class.getSimpleName() + ".json";
        JcrQueryManagerTest.beforeAll(configFileName);
    }

    @Override
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithCompoundCriteria() throws Exception {
        //overrides the default to add explicit property name in FTS @car:engine, otherwise the 'textFromCarMaker' index would be used
        //and no results would be returned
        String xpath = "/jcr:root/Cars//element(*,car:Car)[@car:year='2008' and jcr:contains(@car:engine, '\"liters V 12\"')]";
        Query query = getSession().getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        String[] columnNames = {"jcr:primaryType", "jcr:mixinTypes", "jcr:path", "jcr:score", "jcr:created", "jcr:createdBy",
                                "jcr:name", "mode:localName", "mode:depth", "mode:id", "car:mpgCity", "car:userRating", "car:mpgHighway",
                                "car:engine", "car:model", "car:year", "car:maker", "car:lengthInInches", "car:valueRating", "car:wheelbaseInInches",
                                "car:msrp", "car:alternateModels"};
        validateQuery().rowCount(1).hasColumns(columnNames).validate(query, result);

        // Query again with a different criteria that should return no nodes ...
        xpath = "/jcr:root/Cars//element(*,car:Car)[@car:year='2007' and jcr:contains(@car:engine, '\"liter V 12\"')]";
        query = getSession().getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = query.execute();
        validateQuery().rowCount(0).hasColumns(columnNames).validate(query, result);
    }
}
