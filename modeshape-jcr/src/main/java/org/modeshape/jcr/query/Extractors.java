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

package org.modeshape.jcr.query;

import org.modeshape.jcr.query.NodeSequence.ExtractFromRow;
import org.modeshape.jcr.query.NodeSequence.RowAccessor;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class Extractors {

    public static ExtractFromRow convert( final ExtractFromRow extractor,
                                          final TypeFactory<?> newType ) {
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return newType;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                Object value = extractor.getValueInRow(row);
                if (value == null) return value;
                if (value instanceof Object[]) {
                    Object[] values = (Object[])value;
                    for (int i = 0; i != values.length; ++i) {
                        values[i] = newType.create(values[i]);
                    }
                    return values;
                }
                return newType.create(extractor.getValueInRow(row));
            }

            @Override
            public String toString() {
                return "(as-string " + extractor.toString() + " )";
            }
        };
    }

    private Extractors() {
    }

}
