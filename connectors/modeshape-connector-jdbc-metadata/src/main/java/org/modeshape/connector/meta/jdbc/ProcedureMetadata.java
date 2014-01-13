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
package org.modeshape.connector.meta.jdbc;

/**
 * Container for column-level metadata. The fields in this class roughly parallel the information returned from the
 * {@link java.sql.DatabaseMetaData#getProcedures(String, String, String)} method.
 */
public class ProcedureMetadata {

    private final String name;
    private final String description;
    private final int type;

    protected ProcedureMetadata( String name,
                                 String description,
                                 int type ) {
        super();
        this.name = name;
        this.description = description;
        this.type = type;
    }

    /**
     * @return the procedure name (PROCEDURE_NAME in the {@link java.sql.DatabaseMetaData#getProcedures(String, String, String)} result
     *         set).
     */
    public String getName() {
        return name;
    }

    /**
     * @return the procedure description (REMARKS in the {@link java.sql.DatabaseMetaData#getProcedures(String, String, String)} result
     *         set).
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the kind of procedure (PROCEDURE_TYPE in the {@link java.sql.DatabaseMetaData#getProcedures(String, String, String)} result
     *         set).
     */
    public int getType() {
        return type;
    }

}
