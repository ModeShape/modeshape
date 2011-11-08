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

import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.delta.DocumentObserver;
import org.infinispan.schematic.internal.delta.PutOperation;
import org.infinispan.schematic.internal.delta.RemoveOperation;

public class ObservableDocumentEditor extends DocumentEditor {

    private static final long serialVersionUID = 1L;

    private final Path path;
    private final DocumentObserver observer;

    public ObservableDocumentEditor( MutableDocument document,
                                     Path path,
                                     DocumentObserver delta,
                                     DocumentValueFactory factory ) {
        super(document, factory);
        this.path = path;
        this.observer = delta;
    }

    @Override
    protected Object doSetValue( String name,
                                 Object newValue ) {
        Object oldValue = super.doSetValue(name, newValue);
        observer.addOperation(new PutOperation(path, name, copy(oldValue), copy(newValue)));
        return oldValue;
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
