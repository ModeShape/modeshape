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
package org.modeshape.connector;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.basic.BasicName;

/**
 * Lexicon of names used by CMIS connector.
 *
 * @author kulikov
 */
@Immutable
public class CmisLexicon {
    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/cmis/1.0";
        public static final String PREFIX = "cmis";
    }

    public static final Name OBJECT_ID = new BasicName(Namespace.URI, "objectId");

    public static final Name REPOSITORY = new BasicName(Namespace.URI, "repository");

    public static final Name FOLDER = new BasicName(Namespace.URI, "folder");
    public static final Name DOCUMENT = new BasicName(Namespace.URI, "document");
    public static final Name POLICY = new BasicName(Namespace.URI, "policy");
    public static final Name RELATIONSHIP = new BasicName(Namespace.URI, "relationship");

    public static final Name VENDOR_NAME = new BasicName(Namespace.URI, "vendorName");
    public static final Name PRODUCT_NAME = new BasicName(Namespace.URI, "productName");
    public static final Name PRODUCT_VERSION = new BasicName(Namespace.URI, "productVersion");

    public static final Name CREATED_BY = new BasicName(Namespace.URI, "createdBy");
    public static final Name CREATION_DATE = new BasicName(Namespace.URI, "creationDate");
    public static final Name LAST_MODIFIED_BY = new BasicName(Namespace.URI, "lastModifiedBy");
    public static final Name LAST_MODIFICATION_DATE = new BasicName(Namespace.URI, "lastModificationDate");

    public static final Name DATA = new BasicName(Namespace.URI, "data");


}
