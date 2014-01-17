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
package org.modeshape.jdbc;

import java.sql.RowId;

public class JcrRowId implements RowId {

    private final String path;
    private final byte[] bytes;

    public JcrRowId( String path ) {
        this.path = path;
        this.bytes = path.getBytes(); // don't make a copy
    }

    public String getPath() {
        return path;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
