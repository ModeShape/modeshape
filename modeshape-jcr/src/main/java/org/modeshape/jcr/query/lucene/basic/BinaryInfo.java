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
package org.modeshape.jcr.query.lucene.basic;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.modeshape.common.annotation.Immutable;

/**
 * 
 */
@Immutable
@Indexed( index = BinaryInfoIndex.INDEX_NAME )
public class BinaryInfo {

    /**
     * The SHA-1 hash (in hexadecimal format) of the binary content. Note that DocumentId fields are stored, meaning we can get
     * them back out of the index.
     */
    @DocumentId( name = BinaryInfoIndex.FieldName.SHA1 )
    private final String sha1;

    @Field( name = BinaryInfoIndex.FieldName.FULL_TEXT, analyze = Analyze.YES, store = Store.NO, index = Index.YES )
    private final String text;

    public BinaryInfo( String sha1,
                       String text ) {
        this.sha1 = sha1;
        this.text = text;
    }

    /**
     * @return sha1
     */
    public String getSha1() {
        return sha1;
    }

    /**
     * @return text
     */
    public String getText() {
        return text;
    }
}
