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

package org.modeshape.jcr.index.local;

import java.util.Comparator;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.Serializer;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.index.local.IndexValues.Converter;
import org.modeshape.jcr.index.local.MapDB.Serializers;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ValueFactory;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class IndexSpec {

    public static IndexSpec create( ExecutionContext context,
                                    IndexDefinition defn ) {
        if (defn.hasSingleColumn()) {
            return new SingleColumnSpec(context, defn);
        }
        return null;
    }

    protected final ExecutionContext context;
    protected final Serializers serializers;

    protected IndexSpec( ExecutionContext context,
                         IndexDefinition defn ) {
        this.context = context;
        serializers = MapDB.serializers(this.context.getValueFactories());
    }

    public abstract Serializer<?> getSerializer();

    public abstract BTreeKeySerializer<?> getBTreeKeySerializer();

    public abstract Comparator<?> getComparator();

    public abstract Converter<?> getConverter();

    public abstract Class<?> getValueClass();

    protected static class SingleColumnSpec extends IndexSpec {
        private final IndexColumnDefinition columnDefn;
        private final PropertyType type;

        protected SingleColumnSpec( ExecutionContext context,
                                    IndexDefinition defn ) {
            super(context, defn);
            assert defn.hasSingleColumn();
            columnDefn = defn.getColumnDefinition(0);
            type = PropertyType.valueFor(columnDefn.getColumnType());
        }

        @Override
        public Serializer<?> getSerializer() {
            return serializers.serializerFor(getValueClass());
        }

        @Override
        public BTreeKeySerializer<?> getBTreeKeySerializer() {
            return serializers.bTreeKeySerializerFor(getValueClass(), getComparator(), false);
        }

        @Override
        public Comparator<?> getComparator() {
            return getColumnType().getComparator();
        }

        @Override
        public Class<?> getValueClass() {
            return getColumnType().getValueClass();
        }

        protected PropertyType getColumnType() {
            return type;
        }

        @Override
        public Converter<?> getConverter() {
            ValueFactory<?> factory = this.context.getValueFactories().getValueFactory(getColumnType());
            return IndexValues.converter(factory);
        }
    }

}
