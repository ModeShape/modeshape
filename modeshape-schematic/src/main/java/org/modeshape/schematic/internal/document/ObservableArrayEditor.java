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

import java.util.Collection;
import java.util.List;
import org.modeshape.schematic.document.Array;
import org.modeshape.schematic.document.EditableArray;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Path;
import org.modeshape.schematic.internal.delta.AddValueIfAbsentOperation;
import org.modeshape.schematic.internal.delta.AddValueOperation;
import org.modeshape.schematic.internal.delta.ClearOperation;
import org.modeshape.schematic.internal.delta.DocumentObserver;
import org.modeshape.schematic.internal.delta.RemoveAllValuesOperation;
import org.modeshape.schematic.internal.delta.RemoveAtIndexOperation;
import org.modeshape.schematic.internal.delta.RemoveValueOperation;
import org.modeshape.schematic.internal.delta.RetainAllValuesOperation;
import org.modeshape.schematic.internal.delta.SetValueOperation;

public class ObservableArrayEditor extends ArrayEditor {

    private static final long serialVersionUID = 1L;

    private final Path path;
    private final DocumentObserver observer;

    public ObservableArrayEditor( MutableArray array,
                                  Path path,
                                  DocumentObserver observer,
                                  DocumentValueFactory factory ) {
        super(array, factory);
        this.path = path;
        this.observer = observer;
    }

    @Override
    protected boolean doAddAll( Collection<?> c ) {
        return doAddAll(size(), c);
    }

    @Override
    protected boolean doAddAll( int index,
                                Collection<?> c ) {
        c = Utility.unwrapValues(c);
        if (super.doAddAll(index, c)) {
            for (Object value : c) {
                value = Utility.unwrap(value);
                observer.addOperation(new AddValueOperation(this.path, value));
            }
            return true;
        }
        return false;
    }

    @Override
    protected void doAddValue( int index,
                               Object value ) {
        value = Utility.unwrap(value);
        super.doAddValue(index, value);
        observer.addOperation(new AddValueOperation(this.path, value, index));
    }

    @Override
    protected int doAddValue( Object value ) {
        value = Utility.unwrap(value);
        int index = super.doAddValue(value);
        observer.addOperation(new AddValueOperation(this.path, value));
        return index;
    }

    @Override
    protected boolean doAddValueIfAbsent( Object value ) {
        value = Utility.unwrap(value);
        if (super.doAddValueIfAbsent(value)) {
            observer.addOperation(new AddValueIfAbsentOperation(this.path, value));
            return true;
        }
        return false;
    }

    @Override
    protected void doClear() {
        super.doClear();
        observer.addOperation(new ClearOperation(this.path));
    }

    @Override
    protected List<Array.Entry> doRemoveAll( Collection<?> c ) {
        c = Utility.unwrapValues(c);
        List<Array.Entry> removed = super.doRemoveAll(c);
        observer.addOperation(new RemoveAllValuesOperation(this.path, c));
        return removed;
    }

    @Override
    protected Object doRemoveValue( int index ) {
        Object removed = super.doRemoveValue(index);
        if (removed != null) {
            observer.addOperation(new RemoveAtIndexOperation(this.path, index));
        }
        return removed;
    }

    @Override
    protected boolean doRemoveValue( Object value ) {
        value = Utility.unwrap(value);
        if (super.doRemoveValue(value)) {
            observer.addOperation(new RemoveValueOperation(this.path, value));
        }
        return false;
    }

    @Override
    protected List<Array.Entry> doRetainAll( Collection<?> c ) {
        c = Utility.unwrapValues(c);
        List<Array.Entry> removed = super.doRetainAll(c);
        observer.addOperation(new RetainAllValuesOperation(this.path, c));
        return removed;
    }

    @Override
    protected Object doSetValue( int index,
                                 Object value ) {
        value = Utility.unwrap(value);
        Object oldValue = super.doSetValue(index, value);
        observer.addOperation(new SetValueOperation(path, value, index));
        return oldValue;
    }

    @Override
    protected EditableDocument createEditableDocument( MutableDocument document,
                                                       int index,
                                                       DocumentValueFactory factory ) {
        return new ObservableDocumentEditor(document, path.with(Integer.toString(index)), observer, factory);
    }

    @Override
    protected EditableArray createEditableArray( MutableArray array,
                                                 int index,
                                                 DocumentValueFactory factory ) {
        return new ObservableArrayEditor(array, path.with(Integer.toString(index)), observer, factory);
    }

    @Override
    protected EditableArray createEditableSublist( MutableArray array,
                                                   DocumentValueFactory factory ) {
        return new ObservableArrayEditor(array, path, observer, factory);
    }
}
