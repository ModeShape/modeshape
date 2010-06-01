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
package org.modeshape.graph.property;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Path.Segment;

/**
 * Tests that the {@link PathFactory} are producing valid paths. The tests with comments citing a section refer to the JCR 2.0
 * specification.
 */
public class PathFactoryTest {

    private ExecutionContext context;
    private PathFactory paths;
    private Path path;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        context.getNamespaceRegistry().register("ex", "http://www.example.com");
        paths = context.getValueFactories().getPathFactory();
    }

    public Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    public Segment segment( String segment ) {
        return paths.createSegment(segment);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Root paths
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldCreateRootPath() {
        assertThat(paths.createRootPath(), is(notNullValue()));
    }

    @Test
    public void shouldCreateRootPathWithNoSegments() {
        assertThat(paths.createRootPath().getSegmentsList().isEmpty(), is(true));
        assertThat(paths.createRootPath().getSegmentsList().size(), is(0));
        assertThat(paths.createRootPath().getSegmentsArray().length, is(0));
        assertThat(paths.createRootPath().size(), is(0));
    }

    @Test
    public void shouldCreateRootPathWithLengthOfZero() {
        assertThat(paths.createRootPath().size(), is(0));
    }

    @Test
    public void shouldCreateRootPathThatIsAbsolute() {
        assertThat(paths.createRootPath().isAbsolute(), is(true));
    }

    @Test
    public void shouldCreateRootPathThatHasNoParent() {
        assertThat(paths.createRootPath().getParent(), is(nullValue()));
    }

    @Test
    public void shouldReturnRootPathsThatAreAlwaysEquivalent() {
        path = paths.createRootPath();
        for (int i = 0; i != 10; ++i) {
            assertThat(paths.createRootPath(), is(path));
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Relative paths
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldCreateRelativePathWithOnlySelf() {
        path = paths.create(".");
        assertThat(path.isAbsolute(), is(false));
        assertThat(path.getSegment(0), is(Path.SELF_SEGMENT));
        assertThat(path.size(), is(1));
    }

    @Test
    public void shouldCreateRelativePathWithOnlySelfSegment() {
        path = paths.createRelativePath(name("."));
        assertThat(path.isAbsolute(), is(false));
        assertThat(path.getSegment(0), is(Path.SELF_SEGMENT));
        assertThat(path.size(), is(1));
    }

    @Test
    public void shouldCreateRelativePathWithSingleSegment() {
        path = paths.create("A");
        assertThat(path.isAbsolute(), is(false));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.size(), is(1));
    }

    @Test
    public void shouldCreateRelativePathWithMultipleSegment() {
        path = paths.create("A/B/C[1]/D[2]");
        assertThat(path.isAbsolute(), is(false));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(segment("D[2]")));
        assertThat(path.size(), is(4));
    }

    @Test
    public void shouldCreateRelativePathWithMultipleSegmentIncludingParent() {
        path = paths.create("A/B/C[1]/../../D[2]");
        assertThat(path.isAbsolute(), is(false));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(Path.PARENT_SEGMENT));
        assertThat(path.getSegment(4), is(Path.PARENT_SEGMENT));
        assertThat(path.getSegment(5), is(segment("D[2]")));
        assertThat(path.size(), is(6));
    }

    @Test
    public void shouldCreateRelativePathWithMultipleSegmentIncludingSelf() {
        path = paths.create("A/B/C[1]/././D[2]");
        assertThat(path.isAbsolute(), is(false));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(Path.SELF_SEGMENT));
        assertThat(path.getSegment(4), is(Path.SELF_SEGMENT));
        assertThat(path.getSegment(5), is(segment("D[2]")));
        assertThat(path.size(), is(6));
    }

    @Test
    public void shouldCreateRelativePathWithMultipleSegmentIncludingSelfAndParent() {
        path = paths.create("A/B/C[1]/../D[2]/./E/..");
        assertThat(path.isAbsolute(), is(false));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(Path.PARENT_SEGMENT));
        assertThat(path.getSegment(4), is(segment("D[2]")));
        assertThat(path.getSegment(5), is(Path.SELF_SEGMENT));
        assertThat(path.getSegment(6), is(segment("E[1]")));
        assertThat(path.getSegment(7), is(Path.PARENT_SEGMENT));
        assertThat(path.size(), is(8));
    }

    @Test
    public void shouldNormalizeRelativePathWithMultipleSegmentIncludingSelfAndParent() {
        assertThat(paths.create("A/B/C[1]/../D[2]/./E/..").getNormalizedPath(), is(paths.create("A/B/D[2]")));
        assertThat(paths.create("A/B/C[1]/././D[2]").getNormalizedPath(), is(paths.create("A/B/C/D[2]")));
        assertThat(paths.create("A/../../../B").getNormalizedPath(), is(paths.create("../../B")));
        assertThat(paths.create("../../B").getNormalizedPath(), is(paths.create("../../B")));
        assertThat(paths.create("./B").getNormalizedPath(), is(paths.create("B")));
        assertThat(paths.create("A/./../../B/.").getNormalizedPath(), is(paths.create("../B")));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Absolute paths
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldCreateAbsolutePathWithOnlySelf() {
        path = paths.create("/.");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(Path.SELF_SEGMENT));
        assertThat(path.size(), is(1));
    }

    @Test
    public void shouldCreateAbsolutePathWithOnlySelfSegment() {
        path = paths.create(name("."));
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(Path.SELF_SEGMENT));
        assertThat(path.size(), is(1));
    }

    @Test
    public void shouldCreateAbsolutePathWithSingleSegment() {
        path = paths.create("/A");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.size(), is(1));
    }

    @Test
    public void shouldCreateAbsolutePathWithMultipleSegment() {
        path = paths.create("/A/B/C[1]/D[2]");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(segment("D[2]")));
        assertThat(path.size(), is(4));
    }

    @Test
    public void shouldCreateAbsolutePathWithMultipleSegmentIncludingParent() {
        path = paths.create("/A/B/C[1]/../../D[2]");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(Path.PARENT_SEGMENT));
        assertThat(path.getSegment(4), is(Path.PARENT_SEGMENT));
        assertThat(path.getSegment(5), is(segment("D[2]")));
        assertThat(path.size(), is(6));
    }

    @Test
    public void shouldCreateAbsolutePathWithMultipleSegmentIncludingSelf() {
        path = paths.create("/A/B/C[1]/././D[2]");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(Path.SELF_SEGMENT));
        assertThat(path.getSegment(4), is(Path.SELF_SEGMENT));
        assertThat(path.getSegment(5), is(segment("D[2]")));
        assertThat(path.size(), is(6));
    }

    @Test
    public void shouldCreateAbsolutePathWithMultipleSegmentIncludingSelfAndParent() {
        path = paths.create("/A/B/C[1]/../D[2]/./E/..");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(Path.PARENT_SEGMENT));
        assertThat(path.getSegment(4), is(segment("D[2]")));
        assertThat(path.getSegment(5), is(Path.SELF_SEGMENT));
        assertThat(path.getSegment(6), is(segment("E[1]")));
        assertThat(path.getSegment(7), is(Path.PARENT_SEGMENT));
        assertThat(path.size(), is(8));
    }

    @Test
    public void shouldNormalizeAbsolutePathWithMultipleSegmentIncludingSelfAndParent() {
        assertThat(paths.create("/A/B/C[1]/../D[2]/./E/..").getNormalizedPath(), is(paths.create("/A/B/D[2]")));
        assertThat(paths.create("/A/B/C[1]/././D[2]").getNormalizedPath(), is(paths.create("/A/B/C/D[2]")));
        assertThat(paths.create("/./B").getNormalizedPath(), is(paths.create("/B")));
        assertThat(paths.create("/./././B").getNormalizedPath(), is(paths.create("/B")));
    }

    @Test( expected = InvalidPathException.class )
    public void shouldFailToCreateAbsolutePathWithMoreParentReferencesThanDepth() {
        paths.create("/A/../../../B").getNormalizedPath();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Lexical form
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldParsePathFromStringWithRootSegment() {
        assertThat(paths.create("/").isRoot(), is(true));
    }

    @Test
    public void shouldParsePathFromStringWithRootSegmentAndSelfSegment() {
        path = paths.create("/.");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(Path.SELF_SEGMENT));
        assertThat(path.size(), is(1));
    }

    @Test
    public void shouldParsePathFromStringWithSingleChildSegment() {
        path = paths.create("/A");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.size(), is(1));
    }

    @Test
    public void shouldParsePathFromStringWithMultipleChildSegments() {
        path = paths.create("/A/B/C");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.size(), is(3));
    }

    @Test
    public void shouldParsePathFromStringWithSingleChildSegmentWithSameNameSiblingIndex() {
        path = paths.create("/A[2]");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[2]")));
        assertThat(path.size(), is(1));
    }

    @Test
    public void shouldParsePathFromStringWithMultipleChildSegmentsWithSameNameSiblingIndex() {
        path = paths.create("/A/B[2]/C");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[2]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.size(), is(3));
    }

    @Test
    public void shouldParsePathFromStringWithTrailingSlash() {
        path = paths.create("/A/B/C/D/");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(segment("D[1]")));
        assertThat(path.size(), is(4));
    }

    @Test
    public void shouldParsePathFromStringWithoutTrailingSlash() {
        path = paths.create("/A/B/C/D");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(segment("D[1]")));
        assertThat(path.size(), is(4));
    }

    @Test
    public void shouldParsePathFromStringEndingWithSameNameSiblingIndexButWithoutTrailingSlash() {
        path = paths.create("/A/B/C/D[3]");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(segment("D[3]")));
        assertThat(path.size(), is(4));
    }

    @Test
    public void shouldParsePathFromStringWithRootSegmentAndIdentifierSegment() {
        path = paths.create("/A/B/C/D[3]");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(segment("D[3]")));
        assertThat(path.size(), is(4));
    }

    @Test
    public void shouldParsePathFromStringWithNamespacesAndWithSingleChildSegment() {
        path = paths.create("/ex:A");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("ex:A[1]")));
        assertThat(path.size(), is(1));
    }

    @Test
    public void shouldParsePathFromStringWithNamespacesAndWithMultipleChildSegments() {
        path = paths.create("/ex:A/B/ex:C");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("ex:A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("ex:C[1]")));
        assertThat(path.size(), is(3));
    }

    @Test
    public void shouldParsePathFromStringWithNamespacesAndWithSingleChildSegmentWithSameNameSiblingIndex() {
        path = paths.create("/ex:A[2]");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("ex:A[2]")));
        assertThat(path.size(), is(1));
    }

    @Test
    public void shouldParsePathFromStringWithNamespacesAndWithMultipleChildSegmentsWithSameNameSiblingIndex() {
        path = paths.create("/ex:A/ex:B[2]/ex:C");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("ex:A[1]")));
        assertThat(path.getSegment(1), is(segment("ex:B[2]")));
        assertThat(path.getSegment(2), is(segment("ex:C[1]")));
        assertThat(path.size(), is(3));
    }

    @Test
    public void shouldParsePathFromStringWithNamespacesAndWithTrailingSlash() {
        path = paths.create("/A/B/C/D/");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(segment("D[1]")));
        assertThat(path.size(), is(4));
    }

    @Test
    public void shouldParsePathFromStringWithNamespacesAndWithoutTrailingSlash() {
        path = paths.create("/ex:A/ex:B/ex:C/ex:D");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("ex:A[1]")));
        assertThat(path.getSegment(1), is(segment("ex:B[1]")));
        assertThat(path.getSegment(2), is(segment("ex:C[1]")));
        assertThat(path.getSegment(3), is(segment("ex:D[1]")));
        assertThat(path.size(), is(4));
    }

    @Test
    public void shouldParsePathFromStringWithNamespacesAndEndingWithSameNameSiblingIndexButWithoutTrailingSlash() {
        path = paths.create("/A/ex:B/ex:C/ex:D[3]");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("ex:B[1]")));
        assertThat(path.getSegment(2), is(segment("ex:C[1]")));
        assertThat(path.getSegment(3), is(segment("ex:D[3]")));
        assertThat(path.size(), is(4));
    }

    @Test
    public void shouldParsePathFromStringWithNamespacesAndRootSegmentAndIdentifierSegment() {
        path = paths.create("/A/B/C/D[3]");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getSegment(0), is(segment("A[1]")));
        assertThat(path.getSegment(1), is(segment("B[1]")));
        assertThat(path.getSegment(2), is(segment("C[1]")));
        assertThat(path.getSegment(3), is(segment("D[3]")));
        assertThat(path.size(), is(4));
    }

    @Test
    public void shouldParsePathFromStringWithoutRootSegmentAndIdentifierSegment() {
        path = paths.create("[f81d4fae-7dec-11d0-a765-00a0c91e6bf6]");
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.isIdentifier(), is(true));
        assertThat(path.size(), is(1));
        assertThat(path.getSegment(0), is(segment("[f81d4fae-7dec-11d0-a765-00a0c91e6bf6]")));
        assertThat(path.getSegment(0).getName(), is(name("f81d4fae-7dec-11d0-a765-00a0c91e6bf6")));
        assertThat(path.getSegment(0).getIndex(), is(1));
        assertThat(path.getSegment(0).isIdentifier(), is(true));
    }

}
