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
package org.modeshape.connector.git;

import org.modeshape.common.annotation.Immutable;

/**
 * 
 */
@Immutable
public final class CallSpecification {

    protected static final String DELIMITER_STR = "/";
    protected static final char DELIMITER = DELIMITER_STR.charAt(0);

    private final String id;
    private final String[] parts;
    private final int numParts;
    private final int parameterCount;

    public CallSpecification( String id ) {
        this.id = id;
        this.parts = id.split("[/]");
        assert this.parts.length > 0;
        this.numParts = this.parts.length;
        this.parameterCount = numParts - 1;
    }

    public String getFunctionName() {
        return this.parts[0];
    }

    public int parameterCount() {
        return parameterCount;
    }

    public String parameter( int index ) {
        return parts[index + 1];
    }

    public String parametersAsPath( int fromIndex ) {
        return parametersAsPath(fromIndex, numParts);
    }

    public String parametersAsPath( int fromIndex,
                                    int toIndex ) {
        assert fromIndex < numParts;
        assert toIndex <= numParts;
        StringBuilder sb = new StringBuilder();
        for (int i = fromIndex; i != toIndex; ++i) {
            sb.append(DELIMITER);
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    public String lastParameter() {
        if (numParts == 0) return null;
        return this.parts[numParts - 1];
    }

    public String childId( String childPart ) {
        return id + DELIMITER + childPart;
    }

    public String getParentId() {
        if (numParts == 0) {
            // There is no parent ...
            return "";
        }
        if (numParts == 1) {
            // The parent is the root ...
            return GitRoot.ID;
        }
        StringBuilder sb = new StringBuilder();
        int length = numParts - 1;
        for (int i = 0; i != length; ++i) {
            String part = parts[i];
            sb.append(DELIMITER);
            sb.append(part);
        }
        return sb.toString();
    }
}
