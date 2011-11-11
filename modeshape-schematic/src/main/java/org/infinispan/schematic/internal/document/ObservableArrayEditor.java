/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.schematic.internal.document;

import java.util.Collection;
import java.util.List;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.delta.AddValueIfAbsentOperation;
import org.infinispan.schematic.internal.delta.AddValueOperation;
import org.infinispan.schematic.internal.delta.ClearOperation;
import org.infinispan.schematic.internal.delta.DocumentObserver;
import org.infinispan.schematic.internal.delta.RemoveAllValuesOperation;
import org.infinispan.schematic.internal.delta.RemoveAtIndexOperation;
import org.infinispan.schematic.internal.delta.RemoveValueOperation;
import org.infinispan.schematic.internal.delta.RetainAllValuesOperation;
import org.infinispan.schematic.internal.delta.SetValueOperation;

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
    protected boolean doAddAll( Collection<? extends Object> c ) {
        return doAddAll(size(), c);
    }

    @Override
    protected boolean doAddAll( int index,
                                Collection<? extends Object> c ) {
        if (super.doAddAll(index, c)) {
            for (Object value : c) {
                observer.addOperation(new AddValueOperation(this.path, value));
            }
            return true;
        }
        return false;
    }

    @Override
    protected void doAddValue( int index,
                               Object value ) {
        super.doAddValue(index, value);
        observer.addOperation(new AddValueOperation(this.path, value, index));
    }

    @Override
    protected int doAddValue( Object value ) {
        int index = super.doAddValue(value);
        observer.addOperation(new AddValueOperation(this.path, value));
        return index;
    }

    @Override
    protected boolean doAddValueIfAbsent( Object value ) {
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
    protected List<Entry> doRemoveAll( Collection<?> c ) {
        List<Entry> removed = super.doRemoveAll(c);
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
        if (super.doRemoveValue(value)) {
            observer.addOperation(new RemoveValueOperation(this.path, value));
        }
        return false;
    }

    @Override
    protected List<Entry> doRetainAll( Collection<?> c ) {
        List<Entry> removed = super.doRetainAll(c);
        observer.addOperation(new RetainAllValuesOperation(this.path, c));
        return removed;
    }

    @Override
    protected Object doSetValue( int index,
                                 Object value ) {
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
