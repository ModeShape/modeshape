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

package org.modeshape.jcr.query;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.jcr.PropertyType;
import org.modeshape.common.collection.Collections;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeLexicon;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public final class PseudoColumns {

    public static final class Name {
        public static final String JCR_SCORE = "jcr:score";
        public static final String JCR_PATH = "jcr:path";
        public static final String JCR_NAME = "jcr:name";
        public static final String JCR_UUID = "jcr:uuid";
        public static final String MODE_LOCALNAME = "mode:localName";
        public static final String MODE_DEPTH = "mode:depth";
        public static final String MODE_ID = "mode:id";
    }

    public static final class QualifiedName {
        public static final org.modeshape.jcr.value.Name JCR_SCORE = JcrLexicon.SCORE;
        public static final org.modeshape.jcr.value.Name JCR_PATH = JcrLexicon.PATH;
        public static final org.modeshape.jcr.value.Name JCR_NAME = JcrLexicon.NAME;
        public static final org.modeshape.jcr.value.Name JCR_UUID = JcrLexicon.UUID;
        public static final org.modeshape.jcr.value.Name MODE_LOCALNAME = ModeShapeLexicon.LOCALNAME;
        public static final org.modeshape.jcr.value.Name MODE_DEPTH = ModeShapeLexicon.DEPTH;
        public static final org.modeshape.jcr.value.Name MODE_ID = ModeShapeLexicon.ID;
    }

    public static final class Type {
        public static final int JCR_SCORE = PropertyType.DOUBLE;
        public static final int JCR_PATH = PropertyType.STRING;
        public static final int JCR_NAME = PropertyType.STRING;
        public static final int JCR_UUID = PropertyType.STRING;
        public static final int MODE_LOCALNAME = PropertyType.STRING;
        public static final int MODE_DEPTH = PropertyType.LONG;
        public static final int MODE_ID = PropertyType.STRING;
    }

    public static final class Info {
        private final org.modeshape.jcr.value.Name name;
        private final String stringName;
        private final int type;
        private final org.modeshape.jcr.value.PropertyType propType;

        protected Info( String stringName,
                        org.modeshape.jcr.value.Name jcrScore,
                        int type ) {
            this.name = jcrScore;
            this.stringName = stringName;
            this.type = type;
            this.propType = org.modeshape.jcr.value.PropertyType.valueFor(type);
        }

        public org.modeshape.jcr.value.Name getQualifiedName() {
            return name;
        }

        public String getStringName() {
            return stringName;
        }

        public int getType() {
            return type;
        }

        public org.modeshape.jcr.value.PropertyType getTypeAsEnum() {
            return propType;
        }
    }

    protected static final Set<String> PSEUDO_COLUMNS = Collections.unmodifiableSet(Name.JCR_SCORE, Name.JCR_PATH, Name.JCR_NAME,
                                                                                    Name.JCR_UUID, Name.MODE_LOCALNAME,
                                                                                    Name.MODE_DEPTH, Name.MODE_ID);

    protected static final Set<String> PSEUDO_COLUMNS_WITHOUT_UUID = Collections.unmodifiableSet(Name.JCR_SCORE, Name.JCR_PATH,
                                                                                                 Name.JCR_NAME,
                                                                                                 Name.MODE_LOCALNAME,
                                                                                                 Name.MODE_DEPTH, Name.MODE_ID);

    protected static final Map<org.modeshape.jcr.value.Name, Info> PSEUDO_COLUMN_INFOS;
    protected static final Map<org.modeshape.jcr.value.Name, Info> PSEUDO_COLUMN_INFOS_WITHOUT_UUID;
    static {
        Map<org.modeshape.jcr.value.Name, Info> infos = new HashMap<>();
        infos.put(QualifiedName.JCR_SCORE, new Info(Name.JCR_SCORE, QualifiedName.JCR_SCORE, Type.JCR_SCORE));
        infos.put(QualifiedName.JCR_PATH, new Info(Name.JCR_PATH, QualifiedName.JCR_PATH, Type.JCR_PATH));
        infos.put(QualifiedName.JCR_NAME, new Info(Name.JCR_NAME, QualifiedName.JCR_NAME, Type.JCR_NAME));
        infos.put(QualifiedName.JCR_UUID, new Info(Name.JCR_UUID, QualifiedName.JCR_UUID, Type.JCR_UUID));
        infos.put(QualifiedName.MODE_LOCALNAME, new Info(Name.MODE_LOCALNAME, QualifiedName.MODE_LOCALNAME, Type.MODE_LOCALNAME));
        infos.put(QualifiedName.MODE_DEPTH, new Info(Name.MODE_DEPTH, QualifiedName.MODE_DEPTH, Type.MODE_DEPTH));
        infos.put(QualifiedName.MODE_ID, new Info(Name.MODE_ID, QualifiedName.MODE_ID, Type.MODE_ID));
        Map<org.modeshape.jcr.value.Name, Info> infosWithoutUuid = new HashMap<>(infos);
        infosWithoutUuid.remove(QualifiedName.JCR_UUID);
        PSEUDO_COLUMN_INFOS = java.util.Collections.unmodifiableMap(infos);
        PSEUDO_COLUMN_INFOS_WITHOUT_UUID = java.util.Collections.unmodifiableMap(infosWithoutUuid);
    }

    public static Set<String> allNames() {
        return PSEUDO_COLUMNS;
    }

    public static Set<String> allNamesExceptJcrUuid() {
        return PSEUDO_COLUMNS_WITHOUT_UUID;
    }

    public static Collection<Info> allColumns() {
        return PSEUDO_COLUMN_INFOS.values();
    }

    public static Collection<Info> allColumnsExceptJcrUuid() {
        return PSEUDO_COLUMN_INFOS_WITHOUT_UUID.values();
    }

    public static boolean contains( String columnName,
                                    boolean includeJcrUuid ) {
        if (includeJcrUuid) {
            return PSEUDO_COLUMNS.contains(columnName);
        }
        return PSEUDO_COLUMNS_WITHOUT_UUID.contains(columnName);
    }

    public static boolean contains( org.modeshape.jcr.value.Name columnName,
                                    boolean includeJcrUuid ) {
        if (includeJcrUuid) {
            return PSEUDO_COLUMN_INFOS.containsKey(columnName);
        }
        return PSEUDO_COLUMN_INFOS_WITHOUT_UUID.containsKey(columnName);
    }

    public static boolean isPath( String columnName ) {
        return Name.JCR_PATH.equals(columnName);
    }

    public static boolean isPath( org.modeshape.jcr.value.Name columnName ) {
        return QualifiedName.JCR_PATH.equals(columnName);
    }

    public static boolean isName( String columnName ) {
        return Name.JCR_NAME.equals(columnName);
    }

    public static boolean isName( org.modeshape.jcr.value.Name columnName ) {
        return QualifiedName.JCR_NAME.equals(columnName);
    }

    public static boolean isUuid( String columnName ) {
        return Name.JCR_UUID.equals(columnName);
    }

    public static boolean isUuid( org.modeshape.jcr.value.Name columnName ) {
        return QualifiedName.JCR_UUID.equals(columnName);
    }

    public static boolean isScore( String columnName ) {
        return Name.JCR_SCORE.equals(columnName);
    }

    public static boolean isScore( org.modeshape.jcr.value.Name columnName ) {
        return QualifiedName.JCR_SCORE.equals(columnName);
    }

    public static boolean isLocalName( String columnName ) {
        return Name.MODE_LOCALNAME.equals(columnName);
    }

    public static boolean isLocalName( org.modeshape.jcr.value.Name columnName ) {
        return QualifiedName.MODE_LOCALNAME.equals(columnName);
    }

    public static boolean isDepth( String columnName ) {
        return Name.MODE_DEPTH.equals(columnName);
    }

    public static boolean isDepth( org.modeshape.jcr.value.Name columnName ) {
        return QualifiedName.MODE_DEPTH.equals(columnName);
    }

    public static boolean isId( String columnName ) {
        return Name.MODE_ID.equals(columnName);
    }

    public static boolean isId( org.modeshape.jcr.value.Name columnName ) {
        return QualifiedName.MODE_ID.equals(columnName);
    }

    private PseudoColumns() {
    }

}
