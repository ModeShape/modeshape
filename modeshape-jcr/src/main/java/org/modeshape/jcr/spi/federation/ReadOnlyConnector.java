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
package org.modeshape.jcr.spi.federation;

import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.value.Name;

/**
 * A specialized abstract {@link Connector} class that is readable and can <i>never</i> update content. The connector always
 * throws {@link DocumentStoreException}s whenever it is asked to {@link #storeDocument(Document) store},
 * {@link Connector#updateDocument(DocumentChanges) update}, or {@link #removeDocument(String) remove} documents. Thus, do not subclass if
 * your custom connector can <i>sometimes</i> modify content.
 */
public abstract class ReadOnlyConnector extends Connector {

    @Override
    public final boolean removeDocument( String id ) {
        //this should never really be called, because FederatedDocumentStore performs the check
        throw new UnsupportedOperationException("Connector is readonly");
    }

    @Override
    public final void storeDocument( Document document ) {
        //this should never really be called, because FederatedDocumentStore performs the check
        throw new UnsupportedOperationException("Connector is readonly");
    }

    @Override
    public final void updateDocument( DocumentChanges documentChanges ) {
        //this should never really be called, because FederatedDocumentStore performs the check
        throw new UnsupportedOperationException("Connector is readonly");
    }

    @Override
    public String newDocumentId( String parentId,
                                 Name newDocumentName,
                                 Name newDocumentPrimaryType ) {
        //this should never really be called, because FederatedDocumentStore performs the check
        throw new UnsupportedOperationException("Connector is readonly");
    }

    @Override
    public boolean isReadonly() {
        return true;
    }
}
