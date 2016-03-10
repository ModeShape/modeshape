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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.modeshape.schematic.document.Array;
import org.modeshape.schematic.document.Changes;
import org.modeshape.schematic.document.Editor;
import org.modeshape.schematic.internal.delta.AddValueIfAbsentOperation;
import org.modeshape.schematic.internal.delta.AddValueOperation;
import org.modeshape.schematic.internal.delta.ClearOperation;
import org.modeshape.schematic.internal.delta.Operation;
import org.modeshape.schematic.internal.delta.PutIfAbsentOperation;
import org.modeshape.schematic.internal.delta.PutOperation;
import org.modeshape.schematic.internal.delta.RemoveAllValuesOperation;
import org.modeshape.schematic.internal.delta.RemoveAtIndexOperation;
import org.modeshape.schematic.internal.delta.RemoveOperation;
import org.modeshape.schematic.internal.delta.RemoveValueOperation;
import org.modeshape.schematic.internal.delta.RetainAllValuesOperation;
import org.modeshape.schematic.internal.delta.SetValueOperation;

/**
 * Document editor implementation which allows the editing of a document via a list of incremental changes.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public class IncrementalDocumentEditor extends ObservableDocumentEditor implements Editor {
    
    private static final long serialVersionUID = 1L;
    
    private transient final List<Operation> operations;

    public IncrementalDocumentEditor(MutableDocument document,
                                     List<Operation> operations) {
        super(document, Paths.rootPath(), operations::add, null);
        this.operations = operations;
    }

    @Override
    public Changes getChanges() {
        return new DocumentChanges(operations != null ? operations : Collections.emptyList());
    }

    @Override
    public void apply(Changes changes) {
        apply(changes.clone(), null);
    }

    private static Array.Entry newEntry(int index,
                                        Object value) {
        return new BasicArray.BasicEntry(index, value);
    }

    @Override
    public void apply(Changes changes,
                      Observer observer) {
        if (changes.isEmpty()) {
            return;
        }
        MutableDocument mutable = asMutableDocument();
        for (Operation operation : (DocumentChanges) changes) {
            operation.replay(mutable);
            if (observer != null) {
                if (operation instanceof SetValueOperation) {
                    SetValueOperation op = (SetValueOperation) operation;
                    observer.setArrayValue(op.getParentPath(), newEntry(op.getIndex(), op.getValue()));
                } else if (operation instanceof AddValueOperation) {
                    AddValueOperation op = (AddValueOperation) operation;
                    if (op.getActualIndex() != -1) {
                        observer.addArrayValue(op.getParentPath(), newEntry(op.getActualIndex(), op.getValue()));
                    }
                } else if (operation instanceof AddValueIfAbsentOperation) {
                    AddValueIfAbsentOperation op = (AddValueIfAbsentOperation) operation;
                    if (op.isAdded()) {
                        observer.addArrayValue(op.getParentPath(), newEntry(op.getIndex(), op.getValue()));
                    }
                } else if (operation instanceof RemoveValueOperation) {
                    RemoveValueOperation op = (RemoveValueOperation) operation;
                    if (op.getActualIndex() != -1) {
                        observer.removeArrayValue(op.getParentPath(), newEntry(op.getActualIndex(), op.getRemovedValue()));
                    }
                } else if (operation instanceof RemoveAtIndexOperation) {
                    RemoveAtIndexOperation op = (RemoveAtIndexOperation) operation;
                    observer.removeArrayValue(op.getParentPath(), newEntry(op.getIndex(), op.getRemovedValue()));
                } else if (operation instanceof RetainAllValuesOperation) {
                    RetainAllValuesOperation op = (RetainAllValuesOperation) operation;
                    for (Array.Entry entry : op.getRemovedEntries()) {
                        observer.removeArrayValue(op.getParentPath(), entry);
                    }
                } else if (operation instanceof RemoveAllValuesOperation) {
                    RemoveAllValuesOperation op = (RemoveAllValuesOperation) operation;
                    for (Array.Entry entry : op.getRemovedEntries()) {
                        observer.removeArrayValue(op.getParentPath(), entry);
                    }
                } else if (operation instanceof ClearOperation) {
                    ClearOperation op = (ClearOperation) operation;
                    observer.clear(op.getParentPath());
                } else if (operation instanceof PutOperation) {
                    PutOperation op = (PutOperation) operation;
                    observer.put(op.getParentPath(), op.getFieldName(), op.getNewValue());
                } else if (operation instanceof PutIfAbsentOperation) {
                    PutIfAbsentOperation op = (PutIfAbsentOperation) operation;
                    if (op.isApplied()) {
                        observer.put(op.getParentPath(), op.getFieldName(), op.getNewValue());
                    }
                } else if (operation instanceof RemoveOperation) {
                    RemoveOperation op = (RemoveOperation) operation;
                    if (op.isRemoved()) {
                        observer.remove(op.getParentPath(), op.getFieldName());
                    }
                }
            }
        }
    }

    protected static class DocumentChanges implements Changes, Iterable<Operation> {

        private final List<Operation> operations;

        protected DocumentChanges( List<Operation> operations ) {
            this.operations = operations;
        }

        @Override
        public DocumentChanges clone() {
            List<Operation> newOps = operations.stream().map(Operation::clone).collect(Collectors.toList());
            return new DocumentChanges(newOps);
        }

        @Override
        public Iterator<Operation> iterator() {
            return operations.iterator();
        }

        @Override
        public boolean isEmpty() {
            return operations.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Iterator<Operation> iter = operations.iterator();
            if (iter.hasNext()) {
                sb.append(iter.next());
                while (iter.hasNext()) {
                    sb.append("\n").append(iter.next());
                }
            }
            return sb.toString();
        }
    }
}
