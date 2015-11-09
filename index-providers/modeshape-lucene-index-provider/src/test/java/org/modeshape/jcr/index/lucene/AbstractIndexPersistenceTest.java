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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.basic.ModeShapeDateTime;

/**
 * Base class for testing the CRUD operations on Lucene indexes.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class AbstractIndexPersistenceTest {

    private static final Random RANDOM = new Random();

    protected ExecutionContext context;
    protected LuceneConfig config;
    protected LuceneIndex index;
    
    protected abstract LuceneIndex createIndex( String name );

    @Before
    public void setUp() throws Exception {
        String dir = "target/lucene-index-test";
        FileUtil.delete(dir);
        config = LuceneConfig.onDisk(dir);
        context = new ExecutionContext();
        index = defaultIndex();
    }

    protected LuceneIndex defaultIndex() {
        return createIndex("default");
    }

    @After
    public void tearDown() throws Exception {
        index.shutdown(false);
    }
  
    protected String addMultiplePropertiesToSameNode( LuceneIndex index, String nodeKey, int valuesPerProperty, PropertyType type) {
        List<Object> values = new ArrayList<>();
        IndexedProperty property = newProperty(type);
        String propertyName = property.getName();
        for (int i = 0; i < valuesPerProperty; i++) {
            values.add(property.getValue());
            property = newProperty(type);
        }
        index.add(nodeKey, propertyName, values.toArray());
        return propertyName;
    }
    
    protected IndexedProperty newProperty(PropertyType type) {
        String rnd = UUID.randomUUID().toString().replace("\\-", "_");
        switch (type) {
            case STRING: {
                return new IndexedProperty(type, PropertiesTestUtil.STRING_PROP, "string#" + rnd);                
            }
            case BINARY: {
                return new IndexedProperty(type, PropertiesTestUtil.BINARY_PROP, context.getValueFactories().getBinaryFactory().create(rnd));                
            }
            case NAME: {
                return new IndexedProperty(type, PropertiesTestUtil.NAME_PROP, context.getValueFactories().getNameFactory().create("jcr:" + rnd));
            }
            case PATH: {
                return new IndexedProperty(type, PropertiesTestUtil.PATH_PROP, context.getValueFactories().getPathFactory().create("/a/b/" + rnd));                
            }
            case BOOLEAN: {
                Boolean value = RANDOM.nextInt(2) == 1 ? Boolean.TRUE : Boolean.FALSE;
                return new IndexedProperty(type, PropertiesTestUtil.BOOLEAN_PROP, value);
            }
            case DATE: {
                return new IndexedProperty(type, PropertiesTestUtil.DATE_PROP, new ModeShapeDateTime());
            }
            case DECIMAL: {
                return new IndexedProperty(type, PropertiesTestUtil.DECIMAL_PROP, BigDecimal.valueOf(RANDOM.nextDouble()));
            }
            case DOUBLE: {
                return new IndexedProperty(type, PropertiesTestUtil.DOUBLE_PROP, RANDOM.nextDouble());
            }
            case LONG: {
                return new IndexedProperty(type, PropertiesTestUtil.LONG_PROP, RANDOM.nextLong());
            }
            case REFERENCE: {
                return new IndexedProperty(type, PropertiesTestUtil.REF_PROP, context.getValueFactories().getReferenceFactory().create(new NodeKey(rnd), false));
            }
            case WEAKREFERENCE:  {
                return new IndexedProperty(type, PropertiesTestUtil.WEAK_REF_PROP, context.getValueFactories().getReferenceFactory().create(new NodeKey(rnd), false));
            }
            case SIMPLEREFERENCE: {
                return new IndexedProperty(type, PropertiesTestUtil.SIMPLE_REF_PROP, context.getValueFactories().getReferenceFactory().create(new NodeKey(rnd), false));
            }
            case URI: {
                return new IndexedProperty(type, PropertiesTestUtil.URI_PROP, "http://" + rnd);
            }
            default: {
                throw new IllegalArgumentException("Unknown property type:" + type);
            }
        }
    }
    
    protected static class IndexedProperty {
        private final PropertyType type;
        private final String name;
        private final Object value;

        protected IndexedProperty( PropertyType type, String name, Object value ) {
            this.type = type;
            this.name = name;
            this.value = value;
        }

        protected PropertyType getType() {
            return type;
        }

        protected String getName() {
            return name;
        }

        protected Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("IndexedProperty{");
            sb.append("type=").append(type);
            sb.append(", name='").append(name).append('\'');
            sb.append(", value=").append(value);
            sb.append('}');
            return sb.toString();
        }
    }
}
