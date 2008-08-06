/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph.impl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.jboss.dna.spi.graph.impl.IsPathContaining.hasSegments;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.spi.DnaLexicon;
import org.jboss.dna.spi.graph.InvalidPathException;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class BasicPathTest {

    public static final TextEncoder NO_OP_ENCODER = Path.NO_OP_ENCODER;
    public static final Path ROOT = BasicPath.ROOT;

    private BasicNamespaceRegistry namespaceRegistry;
    private String validNamespaceUri;
    private Path path;
    private Path path2;
    private Path.Segment[] validSegments;
    private List<Path.Segment> validSegmentsList;
    private Name[] validSegmentNames;
    private String validNamespacePrefix;
    private PathValueFactory pathFactory;

    @Before
    public void beforeEach() {
        validNamespacePrefix = DnaLexicon.NAMESPACE_PREFIX;
        validNamespaceUri = DnaLexicon.NAMESPACE_URI;
        validSegmentNames = new Name[] {new BasicName(validNamespaceUri, "a"), new BasicName(validNamespaceUri, "b"),
            new BasicName(validNamespaceUri, "c")};
        validSegments = new Path.Segment[] {new BasicPathSegment(validSegmentNames[0]),
            new BasicPathSegment(validSegmentNames[1]), new BasicPathSegment(validSegmentNames[1])};
        validSegmentsList = new ArrayList<Path.Segment>();
        for (Path.Segment segment : validSegments) {
            validSegmentsList.add(segment);
        }
        path = new BasicPath(validSegmentsList, true);
        namespaceRegistry = new BasicNamespaceRegistry();
        namespaceRegistry.register(validNamespacePrefix, validNamespaceUri);
        StringValueFactory stringValueFactory = new StringValueFactory(Path.DEFAULT_DECODER, Path.DEFAULT_ENCODER);
        NameValueFactory nameValueFactory = new NameValueFactory(namespaceRegistry, Path.DEFAULT_DECODER, stringValueFactory);
        pathFactory = new PathValueFactory(Path.DEFAULT_DECODER, stringValueFactory, nameValueFactory);
    }

    @Test
    public void shouldCreateAbsolutePathFromListOfValidSegments() {
        path = new BasicPath(validSegmentsList, true);
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.isNormalized(), is(true));
        assertThat(path.getSegmentsList(), is(validSegmentsList));
        assertThat(path.size(), is(validSegmentsList.size()));
    }

    @Test
    public void shouldCreateRelativePathFromListOfValidSegments() {
        path = new BasicPath(validSegmentsList, false);
        assertThat(path.isAbsolute(), is(false));
        assertThat(path.isNormalized(), is(true));
        assertThat(path.getSegmentsList(), is(validSegmentsList));
        assertThat(path.size(), is(validSegmentsList.size()));
    }

    @Test
    public void shouldCreateAbsolutePathWithParentSegment() {
        validSegmentsList.add(Path.PARENT_SEGMENT);
        path = new BasicPath(validSegmentsList, true);
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.isNormalized(), is(false));
        assertThat(path.getSegmentsList(), is(validSegmentsList));
        assertThat(path.size(), is(validSegmentsList.size()));
    }

    @Test
    public void shouldCreateRelativePathWithParentSegment() {
        validSegmentsList.add(Path.PARENT_SEGMENT);
        path = new BasicPath(validSegmentsList, false);
        assertThat(path.isAbsolute(), is(false));
        assertThat(path.isNormalized(), is(false));
        assertThat(path.getSegmentsList(), is(validSegmentsList));
        assertThat(path.size(), is(validSegmentsList.size()));
    }

    @Test
    public void shouldCreateAbsolutePathWithSelfSegment() {
        validSegmentsList.add(Path.SELF_SEGMENT);
        path = new BasicPath(validSegmentsList, true);
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.isNormalized(), is(false));
        assertThat(path.getSegmentsList(), is(validSegmentsList));
        assertThat(path.size(), is(validSegmentsList.size()));
    }

    @Test
    public void shouldCreateRelativePathWithSelfSegment() {
        validSegmentsList.add(Path.SELF_SEGMENT);
        path = new BasicPath(validSegmentsList, false);
        assertThat(path.isAbsolute(), is(false));
        assertThat(path.isNormalized(), is(false));
        assertThat(path.getSegmentsList(), is(validSegmentsList));
        assertThat(path.size(), is(validSegmentsList.size()));
    }

    @Test
    public void shouldCreatePathWithNoNamespacePrefixes() {
        path = pathFactory.create("/a/b/c/");
        assertThat(path.size(), is(3));
        assertThat(path, hasSegments(pathFactory, "a", "b", "c"));
    }

    @Test
    public void shouldConstructRelativePath() {
        assertThat(pathFactory.create("a/b/c").isAbsolute(), is(false));
        assertThat(pathFactory.create("a/b/c").isNormalized(), is(true));
        assertThat(pathFactory.create("a/b/c").size(), is(3));
        assertThat(pathFactory.create("a/b/c").getString(namespaceRegistry), is("a/b/c"));
    }

    @Test
    public void shouldConstructRelativePathToSelf() {
        assertThat(pathFactory.create(".").isAbsolute(), is(false));
        assertThat(pathFactory.create(".").size(), is(1));
        assertThat(pathFactory.create("."), hasSegments(pathFactory, Path.SELF));

        assertThat(pathFactory.create("./").isAbsolute(), is(false));
        assertThat(pathFactory.create("./").size(), is(1));
        assertThat(pathFactory.create("./"), hasSegments(pathFactory, Path.SELF));
    }

    @Test
    public void shouldConstructRelativePathToParent() {
        assertThat(pathFactory.create("..").isAbsolute(), is(false));
        assertThat(pathFactory.create("..").size(), is(1));
        assertThat(pathFactory.create(".."), hasSegments(pathFactory, Path.PARENT));

        assertThat(pathFactory.create("../").isAbsolute(), is(false));
        assertThat(pathFactory.create("../").size(), is(1));
        assertThat(pathFactory.create("../"), hasSegments(pathFactory, Path.PARENT));
    }

    @Test
    public void shouldConstructRootPathFromStringWithSingleDelimiter() {
        assertThat(pathFactory.create("/"), is(ROOT));
        assertThat(pathFactory.create("/").isRoot(), is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotConstructPathWithSuccessiveDelimiters() {
        pathFactory.create("///a/b///c//d//");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotConstructPathWithOnlyDelimiters() {
        pathFactory.create("///");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotConstructPathFromNullList() {
        new BasicPath(null, true);
    }

    @Test
    public void shouldConstructPathFromStringAndShouldIgnoreLeadingAndTrailingWhitespace() {
        assertThat(pathFactory.create(" \t /  \t").toString(), is("/"));
    }

    @Test
    public void shouldConstructRelativePathIfSuppliedPathHasNoLeadingDelimiter() {
        assertThat(pathFactory.create("a"), hasSegments(pathFactory, "a"));
    }

    @Test
    public void shouldHaveSizeThatReflectsNumberOfSegments() {
        assertThat(path.size(), is(validSegmentsList.size()));
    }

    @Test
    public void shouldIterateOverAllSegmentsReturnedByList() {
        Iterator<Path.Segment> expectedIter = validSegmentsList.iterator();
        for (Path.Segment segment : path) {
            assertThat(segment, is(expectedIter.next()));
        }

        expectedIter = path.getSegmentsList().iterator();
        for (Path.Segment segment : path) {
            assertThat(segment, is(expectedIter.next()));
        }
    }

    @Test
    public void shouldReturnRootForAncestorOfRoot() {
        assertThat(BasicPath.ROOT.getAncestor(), is(ROOT));
    }

    @Test
    public void shouldReturnAncestorForNodeOtherThanRoot() {
        assertThat(path.getAncestor(), is(pathFactory.create("/dna:a/dna:b")));
        assertThat(path.getAncestor().getAncestor(), is(pathFactory.create("/dna:a")));
        assertThat(path.getAncestor().getAncestor().getAncestor(), is(ROOT));
    }

    @Test
    public void shouldReturnNthDegreeAncestor() {
        assertThat(path.getAncestor(1), is(pathFactory.create("/dna:a/dna:b")));
        assertThat(path.getAncestor(2), is(pathFactory.create("/dna:a")));
        assertThat(path.getAncestor(3), is(ROOT));
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotAllowAncestorDegreeLargerThanSize() {
        path.getAncestor(path.size() + 1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNegativeAncestorDegree() {
        path.getAncestor(-1);
    }

    @Test
    public void shouldReturnRootForAnyAncestorExactDegreeFromRoot() {
        assertThat(path.getAncestor(path.size()), is(ROOT));
        assertThat(ROOT.getAncestor(0), is(ROOT));
    }

    @Test
    public void shouldConsiderRootTheLowestCommonAncestorOfAnyNodeAndRoot() {
        assertThat(path.getCommonAncestor(ROOT), is(ROOT));
        assertThat(ROOT.getCommonAncestor(path), is(ROOT));
    }

    @Test
    public void shouldReturnNullForLowestCommonAncestorWithNullPath() {
        assertThat(path.getCommonAncestor(null), is(nullValue()));
    }

    @Test
    public void shouldFindLowestCommonAncestorBetweenTwoNonRootNodesOnCommonBranch() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path common = pathFactory.create("/a");
        assertThat(path1.getCommonAncestor(path2), is(common));

        path1 = pathFactory.create("/a/b/c");
        path2 = pathFactory.create("/a/b/c/d");
        common = path1;
        assertThat(path1.getCommonAncestor(path2), is(common));

        path1 = pathFactory.create("/a/b/c/x/y/");
        path2 = pathFactory.create("/a/b/c/d/e/f/");
        common = pathFactory.create("/a/b/c");
        assertThat(path1.getCommonAncestor(path2), is(common));
    }

    @Test
    public void shouldConsiderRootTheLowestCommonAncestorOfAnyNodesOnSeparateBrances() {
        Path path1 = pathFactory.create("/x/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path common = ROOT;
        assertThat(path1.getCommonAncestor(path2), is(common));
    }

    @Test
    public void shouldConsiderNodeToBeAncestorOfEveryDecendantNode() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path path3 = pathFactory.create("/x/b/c");
        Path path4 = pathFactory.create("/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z");
        Path common = pathFactory.create("/a");
        assertThat(common.isAncestorOf(path1), is(true));
        assertThat(common.isAncestorOf(path2), is(true));
        assertThat(common.isAncestorOf(path3), is(false));

        assertThat(path1.getAncestor().isAncestorOf(path1), is(true));
        for (int i = 1; i < path1.size(); ++i) {
            assertThat(path1.getAncestor(i).isAncestorOf(path1), is(true));
        }
        for (int i = 1; i < path2.size(); ++i) {
            assertThat(path2.getAncestor(i).isAncestorOf(path2), is(true));
        }
        for (int i = 1; i < path3.size(); ++i) {
            assertThat(path3.getAncestor(i).isAncestorOf(path3), is(true));
        }
        for (int i = 1; i < path4.size(); ++i) {
            assertThat(path4.getAncestor(i).isAncestorOf(path4), is(true));
        }
    }

    @Test
    public void shouldConsiderNodeToBeDecendantOfEveryAncestorNode() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path path3 = pathFactory.create("/x/b/c");
        Path path4 = pathFactory.create("/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z");
        Path common = pathFactory.create("/a");
        assertThat(path1.isDecendantOf(common), is(true));
        assertThat(path2.isDecendantOf(common), is(true));
        assertThat(path3.isDecendantOf(common), is(false));

        assertThat(path1.getAncestor().isAncestorOf(path1), is(true));
        for (int i = 1; i < path1.size(); ++i) {
            assertThat(path1.isDecendantOf(path1.getAncestor(i)), is(true));
        }
        for (int i = 1; i < path2.size(); ++i) {
            assertThat(path2.isDecendantOf(path2.getAncestor(i)), is(true));
        }
        for (int i = 1; i < path3.size(); ++i) {
            assertThat(path3.isDecendantOf(path3.getAncestor(i)), is(true));
        }
        for (int i = 1; i < path4.size(); ++i) {
            assertThat(path4.isDecendantOf(path4.getAncestor(i)), is(true));
        }
    }

    @Test
    public void shouldNotConsiderNodeToBeAncestorOfItself() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path path3 = pathFactory.create("/x/b/c");
        assertThat(path1.isAncestorOf(path1), is(false));
        assertThat(path2.isAncestorOf(path2), is(false));
        assertThat(path3.isAncestorOf(path3), is(false));
        assertThat(ROOT.isAncestorOf(ROOT), is(false));
    }

    @Test
    public void shouldNotConsiderNodeToBeDecendantOfItself() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path path3 = pathFactory.create("/x/b/c");
        assertThat(path1.isDecendantOf(path1), is(false));
        assertThat(path2.isDecendantOf(path2), is(false));
        assertThat(path3.isDecendantOf(path3), is(false));
        assertThat(ROOT.isDecendantOf(ROOT), is(false));
    }

    @Test
    public void shouldNotConsiderRootToBeDecendantOfAnyNode() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path path3 = pathFactory.create("/x/b/c");
        Path common = pathFactory.create("/a");
        assertThat(ROOT.isDecendantOf(path1), is(false));
        assertThat(ROOT.isDecendantOf(path2), is(false));
        assertThat(ROOT.isDecendantOf(path3), is(false));
        assertThat(ROOT.isDecendantOf(common), is(false));
    }

    @Test
    public void shouldConsiderRootToBeAncestorOfAnyNode() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path path3 = pathFactory.create("/x/b/c");
        Path common = pathFactory.create("/a");
        assertThat(ROOT.isAncestorOf(path1), is(true));
        assertThat(ROOT.isAncestorOf(path2), is(true));
        assertThat(ROOT.isAncestorOf(path3), is(true));
        assertThat(ROOT.isAncestorOf(common), is(true));
    }

    @Test
    public void shouldNotConsiderRootToBeAncestorOfItself() {
        assertThat(ROOT.isAncestorOf(ROOT), is(false));
    }

    @Test
    public void shouldNotConsiderRootToBeDecendantOfItself() {
        assertThat(ROOT.isDecendantOf(ROOT), is(false));
    }

    @Test
    public void shouldNeverBeDecendantOfNullPath() {
        assertThat(path.isDecendantOf(null), is(false));
        assertThat(ROOT.isDecendantOf(null), is(false));
    }

    @Test
    public void shouldConsiderNodeToBeAtOrAboveItself() {
        assertThat(path.isAtOrAbove(path), is(true));
    }

    @Test
    public void shouldConsiderNodeToBeAtOrBelowItself() {
        assertThat(path.isAtOrBelow(path), is(true));
    }

    @Test
    public void shouldConsiderAncestorToBeAtOrAboveTheDecendant() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path path3 = pathFactory.create("/x/b/c");
        Path path4 = pathFactory.create("/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z");
        for (int i = 1; i < path1.size(); ++i) {
            assertThat(path1.getAncestor(i).isAtOrAbove(path1), is(true));
        }
        for (int i = 1; i < path2.size(); ++i) {
            assertThat(path2.getAncestor(i).isAtOrAbove(path2), is(true));
        }
        for (int i = 1; i < path3.size(); ++i) {
            assertThat(path3.getAncestor(i).isAtOrAbove(path3), is(true));
        }
        for (int i = 1; i < path4.size(); ++i) {
            assertThat(path4.getAncestor(i).isAtOrAbove(path4), is(true));
        }
    }

    @Test
    public void shouldConsiderDecendantToBeAtOrBelowTheAncestor() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path path3 = pathFactory.create("/x/b/c");
        Path path4 = pathFactory.create("/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z");
        for (int i = 1; i < path1.size(); ++i) {
            assertThat(path1.isAtOrBelow(path1.getAncestor(i)), is(true));
        }
        for (int i = 1; i < path2.size(); ++i) {
            assertThat(path2.isAtOrBelow(path2.getAncestor(i)), is(true));
        }
        for (int i = 1; i < path3.size(); ++i) {
            assertThat(path3.isAtOrBelow(path3.getAncestor(i)), is(true));
        }
        for (int i = 1; i < path4.size(); ++i) {
            assertThat(path4.isAtOrBelow(path4.getAncestor(i)), is(true));
        }
    }

    @Test
    public void shouldNotConsiderAncestorToBeAtOrBelowTheDecendant() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path path3 = pathFactory.create("/x/b/c");
        Path path4 = pathFactory.create("/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z");
        for (int i = 1; i < path1.size(); ++i) {
            assertThat(path1.getAncestor(i).isAtOrBelow(path1), is(false));
        }
        for (int i = 1; i < path2.size(); ++i) {
            assertThat(path2.getAncestor(i).isAtOrBelow(path2), is(false));
        }
        for (int i = 1; i < path3.size(); ++i) {
            assertThat(path3.getAncestor(i).isAtOrBelow(path3), is(false));
        }
        for (int i = 1; i < path4.size(); ++i) {
            assertThat(path4.getAncestor(i).isAtOrBelow(path4), is(false));
        }
    }

    @Test
    public void shouldNotConsiderDecendantToBeAtOrAboveTheAncestor() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path path3 = pathFactory.create("/x/b/c");
        Path path4 = pathFactory.create("/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z");
        for (int i = 1; i < path1.size(); ++i) {
            assertThat(path1.isAtOrAbove(path1.getAncestor(i)), is(false));
        }
        for (int i = 1; i < path2.size(); ++i) {
            assertThat(path2.isAtOrAbove(path2.getAncestor(i)), is(false));
        }
        for (int i = 1; i < path3.size(); ++i) {
            assertThat(path3.isAtOrAbove(path3.getAncestor(i)), is(false));
        }
        for (int i = 1; i < path4.size(); ++i) {
            assertThat(path4.isAtOrAbove(path4.getAncestor(i)), is(false));
        }
    }

    @Test
    public void shouldReturnNullForLastSegmentOfRoot() {
        assertThat(ROOT.getLastSegment(), is(nullValue()));
    }

    @Test
    public void shouldReturnLastSegmentOfNonRootPath() {
        Path path1 = pathFactory.create("/a/y/z");
        Path path2 = pathFactory.create("/a/b/c");
        Path path3 = pathFactory.create("/x/b/c");
        Path path4 = pathFactory.create("/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x");
        assertThat(path1.getLastSegment().getName().getLocalName(), is("z"));
        assertThat(path2.getLastSegment().getName().getLocalName(), is("c"));
        assertThat(path3.getLastSegment().getName().getLocalName(), is("c"));
        assertThat(path4.getLastSegment().getName().getLocalName(), is("x"));
    }

    @Test
    public void shouldNormalizePathWithSelfAndParentReferences() {
        path = pathFactory.create("/a/b/c/../d/./e/../..");
        assertThat(path.isNormalized(), is(false));
        assertThat(path.getNormalizedPath(), hasSegments(pathFactory, "a", "b"));
        assertThat(path.getNormalizedPath().isAbsolute(), is(true));
        assertThat(path.getNormalizedPath().isNormalized(), is(true));

        path = pathFactory.create("a/b/c/../d/./e/../..");
        assertThat(path.isNormalized(), is(false));
        assertThat(path.getNormalizedPath(), hasSegments(pathFactory, "a", "b"));
        assertThat(path.getNormalizedPath().isAbsolute(), is(false));
        assertThat(path.getNormalizedPath().isNormalized(), is(true));
    }

    @Test
    public void shouldAlreadyBeNormalizedIfPathContainsNoParentOrSelfReferences() {
        assertThat(pathFactory.create("/a/b/c/d/e").isNormalized(), is(true));
        assertThat(pathFactory.create("a/b/c/d/e").isNormalized(), is(true));
        assertThat(pathFactory.create("a").isNormalized(), is(true));
        assertThat(pathFactory.create("/a").isNormalized(), is(true));
        assertThat(ROOT.isNormalized(), is(true));
    }

    @Test
    public void shouldNotBeNormalizedIfPathContainsParentOrSelfReferences() {
        assertThat(pathFactory.create("/a/b/c/../d/./e/../..").isNormalized(), is(false));
        assertThat(pathFactory.create("a/b/c/../d/./e/../..").isNormalized(), is(false));
        assertThat(pathFactory.create("a/b/c/./d").isNormalized(), is(false));
        assertThat(pathFactory.create("/a/b/c/../d").isNormalized(), is(false));
        assertThat(pathFactory.create(".").isNormalized(), is(false));
        assertThat(pathFactory.create("/.").isNormalized(), is(false));
    }

    @Test( expected = InvalidPathException.class )
    public void shouldFailToReturnNormalizedPathIfPathContainsReferencesToParentsAboveRoot() {
        path = pathFactory.create("/a/../../../..");
        assertThat(path.isNormalized(), is(false));
        path.getNormalizedPath();
    }

    @Test
    public void shouldReturnRootPathAsTheNormalizedPathForAnAbsolutePathWithZeroSegmentsAfterParentAndSelfReferencesRemoved() {
        // "/a/../b/../c/.." => "/"
        path = pathFactory.create("/a/../b/../c/../");
        assertThat(path.isNormalized(), is(false));
        assertThat(path.getNormalizedPath(), is(ROOT));
    }

    @Test
    public void shouldReturnSelfPathAsTheNormalizedPathForARelativePathWithZeroSegmentsAfterParentAndSelfReferencesRemoved() {
        // "a/../b/../c/.." => "."
        path = pathFactory.create("a/../b/../c/../");
        assertThat(path.isNormalized(), is(false));
        assertThat(path.getNormalizedPath().size(), is(1));
        assertThat(path.getNormalizedPath(), hasSegments(pathFactory, "."));
    }

    @Test
    public void shouldNotHaveAnyParentOrSelfReferencesInTheNormalizedPathOfAnAbsolutePath() {
        path = pathFactory.create("/a/b/c/../d/./e/../..");
        assertThat(path.isNormalized(), is(false));
        assertThat(path.getNormalizedPath(), hasSegments(pathFactory, "a", "b"));
        assertThat(path.getNormalizedPath().isAbsolute(), is(true));
        assertThat(path.getNormalizedPath().isNormalized(), is(true));
    }

    @Test
    public void shouldNotHaveAnyParentReferencesInTheNormalizedPathOfARelativePath() {
        path = pathFactory.create("a/b/c/../d/./e/../..");
        assertThat(path.isNormalized(), is(false));
        assertThat(path.getNormalizedPath(), hasSegments(pathFactory, "a", "b"));
        assertThat(path.getNormalizedPath().isAbsolute(), is(false));
        assertThat(path.getNormalizedPath().isNormalized(), is(true));
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotComputeCanonicalPathOfNodeThatIsNotAbsolute() {
        pathFactory.create("a/b/c/../d/./e/../..").getCanonicalPath();
    }

    @Test
    public void shouldReturnNormalizedPathForTheCanonicalPathOfAbsolutePath() {
        path = pathFactory.create("/a/b/c/../d/./e/../..");
        assertThat(path.isNormalized(), is(false));
        assertThat(path.isAbsolute(), is(true));
        assertThat(path.getCanonicalPath(), hasSegments(pathFactory, "a", "b"));
        assertThat(path.getCanonicalPath().isAbsolute(), is(true));
        assertThat(path.getCanonicalPath().isNormalized(), is(true));
    }

    @Test
    public void shouldReturnSameSegmentsInIteratorAndArrayAndList() {
        testSegmentsByIteratorAndListAndArray("/a/b/c/../d/./e/../..", "a", "b", "c", "..", "d", ".", "e", "..", "..");
        testSegmentsByIteratorAndListAndArray("/a/b/c", "a", "b", "c");
        testSegmentsByIteratorAndListAndArray("a/b/c/../d/./e/../..", "a", "b", "c", "..", "d", ".", "e", "..", "..");
        testSegmentsByIteratorAndListAndArray("a/b/c", "a", "b", "c");
        testSegmentsByIteratorAndListAndArray("");
        testSegmentsByIteratorAndListAndArray(ROOT.getString());
    }

    public void testSegmentsByIteratorAndListAndArray( String pathStr,
                                                       String... expectedSegmentStrings ) {
        path = pathFactory.create(pathStr);
        assertThat(expectedSegmentStrings.length, is(path.size()));
        Path.Segment[] segmentArray = path.getSegmentsArray();
        List<Path.Segment> segmentList = path.getSegmentsList();
        assertThat(segmentArray.length, is(path.size()));
        assertThat(segmentList.size(), is(path.size()));
        Iterator<Path.Segment> iter = path.iterator();
        Iterator<Path.Segment> listIter = segmentList.iterator();
        for (int i = 0; i != path.size(); ++i) {
            Path.Segment expected = pathFactory.createSegment(expectedSegmentStrings[i]);
            assertThat(path.getSegment(i), is(expected));
            assertThat(segmentArray[i], is(expected));
            assertThat(segmentList.get(i), is(expected));
            assertThat(iter.next(), is(expected));
            assertThat(listIter.next(), is(expected));
        }
        assertThat(iter.hasNext(), is(false));
        assertThat(listIter.hasNext(), is(false));
    }

    @Test
    public void shouldGetStringWithNamespaceUrisIfNoNamespaceRegistryIsProvided() {
        path = pathFactory.create("/dna:a/b/dna:c/../d/./dna:e/../..");
        assertThat(path.getString(NO_OP_ENCODER),
                   is("/{http://www.jboss.org/dna}a/{}b/{http://www.jboss.org/dna}c/../{}d/./{http://www.jboss.org/dna}e/../.."));
    }

    @Test
    public void shouldGetStringWithNamespacePrefixesForAllNamesIfNamespaceRegistryIsProvided() {
        path = pathFactory.create("/dna:a/b/dna:c/../d/./dna:e/../..");
        assertThat(path.getString(namespaceRegistry, NO_OP_ENCODER), is("/dna:a/b/dna:c/../d/./dna:e/../.."));
        namespaceRegistry.register("dna2", validNamespaceUri);
        assertThat(path.getString(namespaceRegistry, NO_OP_ENCODER), is("/dna2:a/b/dna2:c/../d/./dna2:e/../.."));
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldFailToReturnSubpathIfStartingIndexIsNegative() {
        path = pathFactory.create("/dna:a/b/dna:c/../d/./dna:e/../..");
        path.subpath(-1);
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldFailToReturnSubpathWithoutEndingIndexIfStartingIndexIsEqualToOrLargerThanSize() {
        path = pathFactory.create("/dna:a/b/dna:c/../d/./dna:e/../..");
        path.subpath(path.size() + 1);
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldFailToReturnSubpathWithEndingIndexIfStartingIndexIsEqualToOrLargerThanSize() {
        path = pathFactory.create("/dna:a/b/dna:c/../d/./dna:e/../..");
        path.subpath(path.size() + 1, path.size() + 2);
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldFailToReturnSubpathIfEndingIndexIsSmallerThanStartingIndex() {
        path = pathFactory.create("/dna:a/b/dna:c/../d/./dna:e/../..");
        path.subpath(2, 1);
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldFailToReturnSubpathIfEndingIndexIsEqualToOrLargerThanSize() {
        path = pathFactory.create("/dna:a/b/dna:c/../d/./dna:e/../..");
        path.subpath(2, path.size() + 1);
    }

    @Test
    public void shouldReturnRootAsSubpathIfStartingIndexAndEndingIndexAreBothZero() {
        path = pathFactory.create("/dna:a/b/dna:c/../d/./dna:e/../..");
        assertThat(path.subpath(0, 0), is(ROOT));
    }

    @Test
    public void shouldReturnSubpathIfValidStartingIndexAndNoEndingIndexAreProvided() {
        path = pathFactory.create("/dna:a/b/dna:c/../d/./dna:e/../..");
        assertThat(path.subpath(0), hasSegments(pathFactory, "dna:a", "b", "dna:c", "..", "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(0), is(path));
        assertThat(path.subpath(0), is(sameInstance(path)));
        assertThat(path.subpath(1), hasSegments(pathFactory, "b", "dna:c", "..", "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(2), hasSegments(pathFactory, "dna:c", "..", "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(3), hasSegments(pathFactory, "..", "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(4), hasSegments(pathFactory, "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(5), hasSegments(pathFactory, ".", "dna:e", "..", ".."));
        assertThat(path.subpath(6), hasSegments(pathFactory, "dna:e", "..", ".."));
        assertThat(path.subpath(7), hasSegments(pathFactory, "..", ".."));
        assertThat(path.subpath(8), hasSegments(pathFactory, ".."));

        path = pathFactory.create("dna:a/b/dna:c/../d/./dna:e/../..");
        assertThat(path.subpath(0), hasSegments(pathFactory, "dna:a", "b", "dna:c", "..", "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(0), is(path));
        assertThat(path.subpath(0), is(sameInstance(path)));
        assertThat(path.subpath(1), hasSegments(pathFactory, "b", "dna:c", "..", "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(2), hasSegments(pathFactory, "dna:c", "..", "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(3), hasSegments(pathFactory, "..", "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(4), hasSegments(pathFactory, "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(5), hasSegments(pathFactory, ".", "dna:e", "..", ".."));
        assertThat(path.subpath(6), hasSegments(pathFactory, "dna:e", "..", ".."));
        assertThat(path.subpath(7), hasSegments(pathFactory, "..", ".."));
        assertThat(path.subpath(8), hasSegments(pathFactory, ".."));
    }

    @Test
    public void shouldReturnSubpathIfValidStartingIndexAndEndingIndexAreProvided() {
        path = pathFactory.create("/dna:a/b/dna:c/../d/./dna:e/../..");
        assertThat(path.subpath(0, path.size()), hasSegments(pathFactory,
                                                             "dna:a",
                                                             "b",
                                                             "dna:c",
                                                             "..",
                                                             "d",
                                                             ".",
                                                             "dna:e",
                                                             "..",
                                                             ".."));
        assertThat(path.subpath(0, path.size()), is(path));
        assertThat(path.subpath(0, path.size()), is(sameInstance(path)));
        assertThat(path.subpath(1, path.size()), hasSegments(pathFactory, "b", "dna:c", "..", "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(2, path.size()), hasSegments(pathFactory, "dna:c", "..", "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(3, path.size()), hasSegments(pathFactory, "..", "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(4, path.size()), hasSegments(pathFactory, "d", ".", "dna:e", "..", ".."));
        assertThat(path.subpath(5, path.size()), hasSegments(pathFactory, ".", "dna:e", "..", ".."));
        assertThat(path.subpath(6, path.size()), hasSegments(pathFactory, "dna:e", "..", ".."));
        assertThat(path.subpath(7, path.size()), hasSegments(pathFactory, "..", ".."));
        assertThat(path.subpath(8, path.size()), hasSegments(pathFactory, ".."));

        assertThat(path.subpath(0, 2), hasSegments(pathFactory, "dna:a", "b"));
        assertThat(path.subpath(1, 2), hasSegments(pathFactory, "b"));
        assertThat(path.subpath(1, 5), hasSegments(pathFactory, "b", "dna:c", "..", "d"));
        assertThat(path.subpath(2, 5), hasSegments(pathFactory, "dna:c", "..", "d"));
        assertThat(path.subpath(3, 5), hasSegments(pathFactory, "..", "d"));
    }

    @Test
    public void shouldFindRelativePaths() {
        path = pathFactory.create("/a/b/c/d");
        assertThat(path.relativeTo(pathFactory.create("/a/e/f")), is(pathFactory.create("../../b/c/d")));
        assertThat(path.relativeTo(pathFactory.create("/e/f")), is(pathFactory.create("../../a/b/c/d")));

    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotAllowFindingRelativePathsFromRelativePaths() {
        path = pathFactory.create("a/b/c/d");
        path.relativeTo(pathFactory.create("/e/f"));

    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotResolveRelativePathToAnotherRelativePath() {
        path = pathFactory.create("/a/b/c/d");
        path.relativeTo(pathFactory.create("e/f"));

    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotResolveRelativePathUsingAnAbsolutePath() {
        path = pathFactory.create("/a/b/c/d");
        path.resolve(pathFactory.create("/e/f"));
    }

    @Test
    public void shouldResolveRelativePathToAbsolutePath() {
        path = pathFactory.create("/a/b/c/d");
        path2 = path.resolve(pathFactory.create("../../e/f"));
        assertThat(path2, is(pathFactory.create("/a/b/e/f")));
        assertThat(path2.isAbsolute(), is(true));
        assertThat(path2.isNormalized(), is(true));
    }

    @Test
    public void shouldOrderPathsCorrectly() {
        List<Path> paths = new ArrayList<Path>();
        paths.add(pathFactory.create("/a"));
        paths.add(pathFactory.create("/a/b"));
        paths.add(pathFactory.create("/a/b/alpha"));
        paths.add(pathFactory.create("/a/b/beta"));
        paths.add(pathFactory.create("/a/b/dna:mixinTypes"));
        paths.add(pathFactory.create("/a/b/dna:name"));
        paths.add(pathFactory.create("/a/b/dna:primaryType"));
        paths.add(pathFactory.create("/a/c[1]"));
        paths.add(pathFactory.create("/a/c[1]/alpha"));
        paths.add(pathFactory.create("/a/c[1]/beta"));
        paths.add(pathFactory.create("/a/c[1]/dna:mixinTypes"));
        paths.add(pathFactory.create("/a/c[1]/dna:name"));
        paths.add(pathFactory.create("/a/c[1]/dna:primaryType"));
        paths.add(pathFactory.create("/a/c[2]"));
        paths.add(pathFactory.create("/a/c[2]/alpha"));
        paths.add(pathFactory.create("/a/c[2]/beta"));
        paths.add(pathFactory.create("/a/c[2]/dna:mixinTypes"));
        paths.add(pathFactory.create("/a/c[2]/dna:name"));
        paths.add(pathFactory.create("/a/c[2]/dna:primaryType"));

        // Randomize the list of paths, so we have something to sort ...
        List<Path> randomizedPaths = new ArrayList<Path>(paths);
        Collections.shuffle(randomizedPaths);
        assertThat(randomizedPaths, is(not(paths)));

        // Sort ...
        Collections.sort(randomizedPaths);
        assertThat(randomizedPaths, is(paths));
    }

    @Test
    public void shouldGetNormalizedPathOfSelfShouldBeSame() {
        assertThat(pathFactory.create(".").getNormalizedPath(), is(pathFactory.create(".")));
        assertThat(pathFactory.create("./").getNormalizedPath(), is(pathFactory.create(".")));
        assertThat(pathFactory.create("./././").getNormalizedPath(), is(pathFactory.create(".")));
    }

    @Test
    public void shouldGetNormalizedPathWithParentReferences() {
        assertThat(pathFactory.create("..").getNormalizedPath(), is(pathFactory.create("..")));
        assertThat(pathFactory.create("../").getNormalizedPath(), is(pathFactory.create("../")));
        assertThat(pathFactory.create("../../../../../..").getNormalizedPath(), is(pathFactory.create("../../../../../..")));
    }

    @Test
    public void shouldGetRelativePathUsingSelf() {
        path = pathFactory.create("/a/b/c/d/e/f");
        assertThat(path.resolve(pathFactory.create(".")), is(sameInstance(path)));
        assertThat(path.resolve(pathFactory.create("././.")), is(sameInstance(path)));
    }

    @Test
    public void shouldResolveRelativePathToParent() {
        path = pathFactory.create("/a/b/c/d/e/f");
        assertThat(path.resolve(pathFactory.create("..")), is(path.getAncestor()));
        assertThat(path.resolve(pathFactory.create("..")), hasSegments(pathFactory, "a", "b", "c", "d", "e"));
    }

    @Test
    public void shouldResolveRelativePaths() {
        path = pathFactory.create("/a/b/c/d/e/f");
        assertThat(path.resolve(pathFactory.create("../../../../../..")), is(sameInstance(ROOT)));
        assertThat(path.resolve(pathFactory.create("../..")), is(path.getAncestor().getAncestor()));
        assertThat(path.resolve(pathFactory.create("../..")), hasSegments(pathFactory, "a", "b", "c", "d"));
        assertThat(path.resolve(pathFactory.create("../x/../y/../z/..")), is(path.getAncestor()));
        assertThat(path.resolve(pathFactory.create("../x/../y/../z/..")), hasSegments(pathFactory, "a", "b", "c", "d", "e"));
        assertThat(path.resolve(pathFactory.create("../x")), hasSegments(pathFactory, "a", "b", "c", "d", "e", "x"));
    }

    public void shouldResolveNonAbsolutePaths() {
        path = pathFactory.create("a/b/c");
        assertThat(path, hasSegments(pathFactory, "a", "b", "c"));
    }
}
