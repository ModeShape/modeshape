/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.sequencer.teiid.lexicon;

import static org.modeshape.sequencer.teiid.lexicon.XmiLexicon.Namespace.PREFIX;

/**
 * Constants associated with the XMI namespace used in reading XMI models and writing JCR nodes.
 */
public interface XmiLexicon {

    /**
     * The URI and prefix constants of the XMI namespace.
     */
    public interface Namespace {
        String PREFIX = "xmi";
        String URI = "http://www.omg.org/XMI";
    }

    /**
     * Constants associated with the XMI namespace that identify XMI model identifiers.
     */
    public interface ModelId {
        String UUID = "uuid";
        String XMI_TAG = "XMI";
    }

    /**
     * JCR identifiers relating to the XMI namespace.
     */
    public interface JcrId {
        String MODEL = PREFIX + ":model";
        String REFERENCEABLE = PREFIX + ":referenceable";
        String UUID = PREFIX + ':' + ModelId.UUID;
        String VERSION = PREFIX + ":version";
        String XMI = PREFIX + ":xmi";
    }
}
