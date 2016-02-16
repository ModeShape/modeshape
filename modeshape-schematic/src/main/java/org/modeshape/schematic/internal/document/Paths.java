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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.modeshape.schematic.annotation.Immutable;
import org.modeshape.schematic.document.Path;

public class Paths {

    protected static final Path EMPTY_PATH = new EmptyPath();

    protected static final void notNull( Object value,
                                         String name ) {
        if (value == null) throw new IllegalArgumentException("The '" + name + "' argument may not be null");
    }

    protected static final String notNull( String value,
                                           String name ) {
        if (value == null) throw new IllegalArgumentException("The '" + name + "' argument may not be null");
        value = value.trim();
        return value;
    }

    public static Path rootPath() {
        return EMPTY_PATH;
    }

    public static Path path( String fieldName ) {
        return new SinglePath(notNull(fieldName, "fieldName"));
    }

    public static Path path( String... fieldNames ) {
        notNull(fieldNames, "fieldNames");
        switch (fieldNames.length) {
            case 0:
                return EMPTY_PATH;
            case 1:
                return new SinglePath(fieldNames[0]);
            default:
                return new MultiSegmentPath(Arrays.asList(fieldNames));
        }
    }

    public static Path path( List<String> fieldNames ) {
        notNull(fieldNames, "fieldNames");
        switch (fieldNames.size()) {
            case 0:
                return EMPTY_PATH;
            case 1:
                return new SinglePath(fieldNames.get(0));
            default:
                // make copy so we're guaranteed to be immutable
                return new MultiSegmentPath(new ArrayList<>(fieldNames));
        }
    }

    public static Path path( Path path,
                             String fieldName ) {
        notNull(path, "path");
        return path.with(notNull(fieldName, "fieldName"));
    }

    public static Path path( Path path,
                             String... fieldNames ) {
        notNull(path, "path");
        notNull(fieldNames, "fieldNames");
        if (fieldNames.length == 0) return path;
        ArrayList<String> names = new ArrayList<>(path.size() + fieldNames.length);
        int i = 0;
        for (String name : path) {
            names.add(notNull(name, "fieldNames[" + i++ + "]"));
        }
        for (String name : fieldNames) {
            names.add(notNull(name, "fieldNames[" + i++ + "]"));
        }
        return new MultiSegmentPath(names);
    }

    public static Path path( Path path,
                             List<String> fieldNames ) {
        notNull(path, "path");
        notNull(fieldNames, "fieldNames");
        if (fieldNames.isEmpty()) return path;
        ArrayList<String> names = new ArrayList<>(path.size() + fieldNames.size());
        int i = 0;
        for (String name : path) {
            names.add(notNull(name, "fieldNames[" + i++ + "]"));
        }
        for (String name : fieldNames) {
            names.add(notNull(name, "fieldNames[" + i++ + "]"));
        }
        return new MultiSegmentPath(names);
    }

    @Immutable
    protected static final class EmptyPath implements Path {
        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public String next() {
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public String get( int index ) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
        }

        @Override
        public String getLast() {
            return null;
        }

        @Override
        public String getFirst() {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean startsWith( Path ancestor ) {
            return ancestor.size() == 0;
        }

        @Override
        public Path with( String fieldName ) {
            return fieldName != null ? new SinglePath(notNull(fieldName, "fieldName")) : this;
        }

        @Override
        public Path parent() {
            return EMPTY_PATH;
        }

        @Override
        public int compareTo( Path that ) {
            if (that == this) return 0;
            if (that instanceof EmptyPath) return 0;
            return 0 - that.size();
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Path) {
                Path that = (Path)obj;
                return that.size() == 0;
            }
            return false;
        }

        @Override
        public String toString() {
            return "";
        }
    }

    @Immutable
    protected static final class SinglePath implements Path {

        private final String fieldName;

        protected SinglePath( String fieldName ) {
            this.fieldName = fieldName;
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                private boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @SuppressWarnings( "synthetic-access" )
                @Override
                public String next() {
                    done = true;
                    return fieldName;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public String get( int index ) {
            if (index != 0) throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
            return fieldName;
        }

        @Override
        public String getLast() {
            return fieldName;
        }

        @Override
        public String getFirst() {
            return fieldName;
        }

        @Override
        public Path with( String fieldName ) {
            return Paths.path(this.fieldName, fieldName);
        }

        @Override
        public Path parent() {
            return EMPTY_PATH;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public int hashCode() {
            return fieldName.hashCode();
        }

        @Override
        public int compareTo( Path that ) {
            if (that == this) return 0;
            int diff = this.size() - that.size();
            if (diff != 0) return diff;
            return this.fieldName.compareTo(that.get(0));
        }

        @Override
        public boolean startsWith( Path ancestor ) {
            return this.equals(ancestor);
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SinglePath) {
                SinglePath that = (SinglePath)obj;
                return this.fieldName.equals(that.fieldName);
            }
            if (obj instanceof Path) {
                Path that = (Path)obj;
                if (this.size() != that.size()) return false;
                return this.fieldName.equals(that.get(0));
            }
            return false;
        }

        @Override
        public String toString() {
            return fieldName;
        }
    }

    @Immutable 
    protected static final class MultiSegmentPath implements Path {

        private final List<String> fieldNames;
        private transient String composite;

        protected MultiSegmentPath( List<String> fieldNames ) {
            assert fieldNames != null;
            assert !fieldNames.isEmpty();
            this.fieldNames = fieldNames;
        }

        @Override
        public Iterator<String> iterator() {
            final Iterator<String> actualIter = fieldNames.iterator();
            return new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    return actualIter.hasNext();
                }

                @Override
                public String next() {
                    return actualIter.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public String get( int index ) {
            return fieldNames.get(index);
        }

        @Override
        public String getLast() {
            return fieldNames.get(fieldNames.size() - 1);
        }

        @Override
        public String getFirst() {
            return fieldNames.get(0);
        }

        @Override
        public int size() {
            return fieldNames.size();
        }

        @Override
        public boolean startsWith( Path other ) {
            if (other.size() > this.size()) return false;
            Iterator<String> thatIter = other.iterator();
            Iterator<String> thisIter = this.iterator();
            while (thatIter.hasNext() && thisIter.hasNext()) {
                if (!thisIter.next().equals(thatIter.next())) return false;
            }
            return !thatIter.hasNext();
        }

        @Override
        public Path with( String fieldName ) {
            fieldName = notNull(fieldName, "fieldName");
            List<String> newFieldNames = new ArrayList<>(fieldNames.size() + 1);
            newFieldNames.addAll(this.fieldNames);
            newFieldNames.add(fieldName);
            return new MultiSegmentPath(newFieldNames);
        }

        @Override
        public Path parent() {
            if (size() <= 1) return EMPTY_PATH;
            List<String> parentFieldNames = this.fieldNames.subList(0, this.fieldNames.size() - 1);
            return new MultiSegmentPath(parentFieldNames);
        }

        @Override
        public int hashCode() {
            return fieldNames.hashCode();
        }

        @Override
        public int compareTo( Path that ) {
            if (that == this) return 0;
            int diff = this.size() - that.size();
            if (diff != 0) return diff;
            Iterator<String> thatIter = that.iterator();
            Iterator<String> thisIter = this.iterator();
            while (thatIter.hasNext()) {
                int value = thisIter.next().compareTo(thatIter.next());
                if (value != 0) return value;
            }
            assert !thisIter.hasNext();
            return 0;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Path) {
                Path that = (Path)obj;
                if (this.size() != that.size()) return false;
                Iterator<String> thatIter = that.iterator();
                Iterator<String> thisIter = this.iterator();
                while (thatIter.hasNext()) {
                    if (!thisIter.next().equals(thatIter.next())) return false;
                }
                assert !thisIter.hasNext();
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            if (composite == null) {
                StringBuilder sb = new StringBuilder();
                Iterator<String> iter = fieldNames.iterator();
                if (iter.hasNext()) {
                    sb.append(iter.next());
                    while (iter.hasNext()) {
                        sb.append('.');
                        sb.append(iter.next());
                    }
                }
                composite = sb.toString();
            }
            return composite;
        }
    }
}
