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
package org.modeshape.schematic.internal.document;

import java.util.Map;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableArray;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Path;
import org.modeshape.schematic.internal.delta.DocumentObserver;
import org.modeshape.schematic.internal.delta.PutIfAbsentOperation;
import org.modeshape.schematic.internal.delta.PutOperation;
import org.modeshape.schematic.internal.delta.RemoveOperation;

public class ObservableDocumentEditor extends DocumentEditor {

    private static final long serialVersionUID = 1L;

    private transient final Path path;
    private transient final DocumentObserver observer;

    public ObservableDocumentEditor( MutableDocument document,
                                     Path path,
                                     DocumentObserver observer,
                                     DocumentValueFactory factory ) {
        super(document, factory);
        this.path = path;
        this.observer = observer;
    }

    @Override
    protected Object doSetValue( String name,
                                 Object newValue ) {
        Object oldValue = super.doSetValue(name, newValue);
        observer.addOperation(new PutOperation(path, name, copy(oldValue), copy(newValue)));
        return oldValue;
    }

    @Override
    protected Object doSetValueIfAbsent( String name,
                                         Object value ) {
        Object oldValue = super.doSetValue(name, value);
        observer.addOperation(new PutIfAbsentOperation(path, name, copy(value)));
        return oldValue;
    }

    @Override
    protected void doSetAllValues( Document values ) {
        if (values != null && !values.isEmpty()) {
            for (Field field : values.fields()) {
                doSetValue(field.getName(), field.getValue());
            }
        }
    }

    @Override
    protected void doSetAllValues( Map<? extends String, ?> values ) {
        if (values != null && !values.isEmpty()) {
            for (Map.Entry<? extends String, ?> entry : values.entrySet()) {
                doSetValue(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public Object remove( String name ) {
        Object oldValue = super.remove(name);
        observer.addOperation(new RemoveOperation(path, name, copy(oldValue)));
        return oldValue;
    }

    protected Object copy( Object value ) {
        if (value instanceof MutableArray) return ((MutableArray)value).clone();
        if (value instanceof MutableDocument) return ((MutableDocument)value).clone();
        return value;
    }

    @Override
    public void removeAll() {
        super.removeAll();
        observer.addOperation(new PutOperation(path.parent(), path.getLast(), copy(unwrap()), new BasicDocument()));
    }

    @Override
    protected EditableDocument createEditableDocument( MutableDocument document,
                                                       String fieldName,
                                                       DocumentValueFactory factory ) {
        return new ObservableDocumentEditor(document, path.with(fieldName), observer, factory);
    }

    @Override
    protected EditableArray createEditableArray( MutableArray array,
                                                 String fieldName,
                                                 DocumentValueFactory factory ) {
        return new ObservableArrayEditor(array, path.with(fieldName), observer, factory);
    }

}
