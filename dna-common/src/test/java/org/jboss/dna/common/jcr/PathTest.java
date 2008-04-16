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
package org.jboss.dna.common.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.dna.common.text.Jsr283Encoder;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class PathTest {

    private Path validPath;

    @Before
    public void beforeEach() throws Exception {
        this.validPath = new Path("/a/b/c/d/e/f");
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotConstructPathFromEmptyString() {
        new Path("");
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotConstructPathFromStringWithOnlyWhitespace() {
        new Path("   \t  ");
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotConstructPathWithSuccessiveDelimiters() {
        new Path("///a/b///c//d//");
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotConstructPathWithOnlyDelimiters() {
        new Path("///");
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotConstructPathFromNullString() {
        new Path((String)null);
    }

    @Test
    public void shouldConstructPathFromStringAndShouldIgnoreLeadingAndTrailingWhitespace() {
        assertThat(new Path(" \t /  \t").toString(), is("/"));
    }

    @Test
    public void shouldConstructRelativePathIfSuppliedPathHasNoLeadingDelimiter() {
        assertThat(new Path("a").toString(), is("a"));
    }

    @Test
    public void shouldConstructRootPathFromStringWithSingleDelimiter() {
        assertThat(new Path("/"), is(Path.ROOT));
        assertThat(new Path("/").isRoot(), is(true));
    }

    @Test
    public void shouldConstructRelativePath() {
        assertThat(new Path("a/b/c").isAbsolute(), is(false));
        assertThat(new Path("a/b/c").isNormalized(), is(true));
        assertThat(new Path("a/b/c").size(), is(3));
        assertThat(new Path("a/b/c").getString(), is("a/b/c"));
        assertThat(new Path("a/b/c").toStringArray(), is(new String[] {"a", "b", "c"}));
    }

    @Test
    public void shouldConstructRelativePathToSelf() {
        assertThat(new Path(".").isAbsolute(), is(false));
        assertThat(new Path(".").size(), is(1));
        assertThat(new Path(".").getSegment(0), is(Path.SELF_SEGMENT));

        assertThat(new Path("./").isAbsolute(), is(false));
        assertThat(new Path("./").size(), is(1));
        assertThat(new Path("./").getSegment(0), is(Path.SELF_SEGMENT));
    }

    @Test
    public void shouldConstructRelativePathToParent() {
        assertThat(new Path("..").isAbsolute(), is(false));
        assertThat(new Path("..").size(), is(1));
        assertThat(new Path("..").getSegment(0), is(Path.PARENT_SEGMENT));

        assertThat(new Path("../").isAbsolute(), is(false));
        assertThat(new Path("../").size(), is(1));
        assertThat(new Path("../").getSegment(0), is(Path.PARENT_SEGMENT));
    }

    @Test
    public void shouldConstructPathCorrectlyFromStrings() {
        assertThat(new Path("/a/b/c/d").toStringArray(), is(new String[] {"a", "b", "c", "d"}));
        assertThat(new Path("/a").toStringArray(), is(new String[] {"a"}));
    }

    @Test
    public void shouldSupportClone() {
        assertThat(validPath.clone(), is(validPath));
        assertThat(validPath.clone(), is(not(sameInstance(validPath))));
    }

    @Test
    public void shouldConstructPathCorrectlyFromStringsWithEncodedCharactersUsingUrlEncoder() {
        assertThat(new Path("/a%3c%20_%2fc", Path.URL_ENCODER).toStringArray(), is(new String[] {"a< _/c"}));
        assertThat(new Path("/%2f", Path.URL_ENCODER).toStringArray(), is(new String[] {"/"}));
        assertThat(new Path("/abcdefghijklmnopqrstuvwxyz", Path.URL_ENCODER).toStringArray(), is(new String[] {"abcdefghijklmnopqrstuvwxyz"}));
        assertThat(new Path("/ABCDEFGHIJKLMNOPQRSTUVWXYZ", Path.URL_ENCODER).toStringArray(), is(new String[] {"ABCDEFGHIJKLMNOPQRSTUVWXYZ"}));
        assertThat(new Path("/0123456789", Path.URL_ENCODER).toStringArray(), is(new String[] {"0123456789"}));
        assertThat(new Path("/-_.!~*\'()", Path.URL_ENCODER).toStringArray(), is(new String[] {"-_.!~*\'()"}));
        assertThat(new Path("/%60%40%23%24%5e%26%7b%5b%7d%5d%7c%3a%3b%22%3c%2c%3e%3f%2f%20", Path.URL_ENCODER).toStringArray(), is(new String[] {"`@#$^&{[}]|:;\"<,>?/ "}));
    }

    @Test
    public void shouldConstructPathCorrectlyFromStringsWithEncodedCharactersUsingJsr283Encoder() {
        assertThat(new Path("/abcdefghijklmnopqrstuvwxyz", Path.JSR283_ENCODER).toStringArray(), is(new String[] {"abcdefghijklmnopqrstuvwxyz"}));
        assertThat(new Path("/ABCDEFGHIJKLMNOPQRSTUVWXYZ", Path.JSR283_ENCODER).toStringArray(), is(new String[] {"ABCDEFGHIJKLMNOPQRSTUVWXYZ"}));
        assertThat(new Path("/0123456789", Path.JSR283_ENCODER).toStringArray(), is(new String[] {"0123456789"}));
        assertThat(new Path("/\uF02F-_.!~\uF02A\'()`@#$^&{\uF05B}\uF05D\uF07C\uF03A;\"<,>?", Path.JSR283_ENCODER).toStringArray(), is(new String[] {"/-_.!~*\'()`@#$^&{[}]|:;\"<,>?"}));
    }

    @Test
    public void shouldConstructPathCorrectlyFromStringsWithEncodedCharactersUsingDefaultEncoder() {
        assertThat(new Path("/abcdefghijklmnopqrstuvwxyz", Path.DEFAULT_ENCODER).toStringArray(), is(new String[] {"abcdefghijklmnopqrstuvwxyz"}));
        assertThat(new Path("/ABCDEFGHIJKLMNOPQRSTUVWXYZ", Path.DEFAULT_ENCODER).toStringArray(), is(new String[] {"ABCDEFGHIJKLMNOPQRSTUVWXYZ"}));
        assertThat(new Path("/0123456789", Path.DEFAULT_ENCODER).toStringArray(), is(new String[] {"0123456789"}));
        assertThat(new Path("/\uF02F-_.!~\uF02A\'()`@#$^&{\uF05B}\uF05D\uF07C\uF03A;\"<,>?", Path.DEFAULT_ENCODER).toStringArray(), is(new String[] {"/-_.!~*\'()`@#$^&{[}]|:;\"<,>?"}));

        assertThat(new Path("/abcdefghijklmnopqrstuvwxyz", null).toStringArray(), is(new String[] {"abcdefghijklmnopqrstuvwxyz"}));
        assertThat(new Path("/ABCDEFGHIJKLMNOPQRSTUVWXYZ", null).toStringArray(), is(new String[] {"ABCDEFGHIJKLMNOPQRSTUVWXYZ"}));
        assertThat(new Path("/0123456789", null).toStringArray(), is(new String[] {"0123456789"}));
        assertThat(new Path("/\uF02F-_.!~\uF02A\'()`@#$^&{\uF05B}\uF05D\uF07C\uF03A;\"<,>?", null).toStringArray(), is(new String[] {"/-_.!~*\'()`@#$^&{[}]|:;\"<,>?"}));

        assertThat(new Path("/abcdefghijklmnopqrstuvwxyz").toStringArray(), is(new String[] {"abcdefghijklmnopqrstuvwxyz"}));
        assertThat(new Path("/ABCDEFGHIJKLMNOPQRSTUVWXYZ").toStringArray(), is(new String[] {"ABCDEFGHIJKLMNOPQRSTUVWXYZ"}));
        assertThat(new Path("/0123456789").toStringArray(), is(new String[] {"0123456789"}));
        assertThat(new Path("/\uF02F-_.!~\uF02A\'()`@#$^&{\uF05B}\uF05D\uF07C\uF03A;\"<,>?").toStringArray(), is(new String[] {"/-_.!~*\'()`@#$^&{[}]|:;\"<,>?"}));
    }

    @Test
    public void shouldUseTheJsr283EncoderForTheDefaultEncoder() {
        assertThat(Path.DEFAULT_ENCODER, is(sameInstance(Path.JSR283_ENCODER)));
        assertThat(Path.DEFAULT_ENCODER, is(instanceOf(Jsr283Encoder.class)));
    }

    @Test
    public void shouldReturnEmptyIteratorForRootPath() {
        assertThat(Path.ROOT.iterator().hasNext(), is(false));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldReturnUnmodifiableList() {
        validPath.toList().remove(1);
    }

    @Test
    public void shouldReturnEquivalentListAndArray() {
        List<Path.Segment> list = validPath.toList();
        Path.Segment[] array = validPath.toArray();
        assertThat(list.size(), is(validPath.size()));
        assertThat(array.length, is(validPath.size()));
        assertThat(list.toArray(), is(equalTo((Object[])array)));
    }

    @Test
    public void shouldConsiderRootToHaveNoElements() {
        assertThat(Path.ROOT.isRoot(), is(true));
        assertThat(Path.ROOT.size(), is(0));
        assertThat(Path.ROOT.toArray(), is(new Path.Segment[] {}));
        assertThat(Path.ROOT.toList(), is((List<Path.Segment>)new ArrayList<Path.Segment>()));
        assertThat(Path.ROOT.getString(), is("/"));
        assertThat(Path.ROOT.getString(Path.DEFAULT_ENCODER), is("/"));
        assertThat(Path.ROOT.getString(Path.JSR283_ENCODER), is("/"));
        assertThat(Path.ROOT.getString(Path.URL_ENCODER), is("/"));
        assertThat(new Path("/"), is(Path.ROOT));
    }

    @Test
    public void shouldReturnSameObjectWhenGettingParentOfRoot() {
        assertThat(Path.ROOT.getAncestor(), is(sameInstance(Path.ROOT)));
    }

    @Test
    public void shouldUseSameSegmentListInstanceInClone() {
        assertThat(validPath.clone().toList(), is(sameInstance(validPath.toList())));
    }

    @Test
    public void shouldConsiderTwoPathsEqualIfTheyHaveTheSamePathSegments() {
        assertThat(new Path("/a/b/c"), is(new Path("/a/b/c")));
        assertThat(new Path("/a/b/c"), is(new Path("/a/b/c/")));
        assertThat(new Path("/"), is(new Path("/")));
    }

    @Test
    public void shouldBeCaseSensitiveInEqualsAndCompareToAndSameAs() {
        assertThat(new Path("/a/b/c"), is(not(new Path("/a/B/c"))));
        assertThat(new Path("/a/b/c").isSame(new Path("/a/B/c")), is(false));
        assertThat(new Path("/a/b/c").equals(new Path("/a/B/c")), is(false));
        assertThat(new Path("/a/b/c").compareTo(new Path("/a/B/c")) != 0, is(true));
    }

    @Test
    public void shouldGetNormalizedPathOfSelfShouldBeSame() {
        assertThat(new Path(".").getNormalizedPath(), is(new Path(".")));
        assertThat(new Path("./").getNormalizedPath(), is(new Path(".")));
        assertThat(new Path("./././").getNormalizedPath(), is(new Path(".")));
    }

    @Test
    public void shouldGetNormalizedPathWithParentReferences() {
        assertThat(new Path("..").getNormalizedPath(), is(new Path("..")));
        assertThat(new Path("../").getNormalizedPath(), is(new Path("../")));
        assertThat(new Path("../../../../../..").getNormalizedPath(), is(new Path("../../../../../..")));
    }

    @Test
    public void shouldGetRelativePathUsingSelf() {
        assertThat(validPath.resolve("."), is(sameInstance(validPath)));
        assertThat(validPath.resolve("././."), is(sameInstance(validPath)));
    }

    @Test
    public void shouldResolveRelativePathToParent() {
        assertThat(validPath.resolve(".."), is(validPath.getAncestor()));
        assertThat(validPath.resolve("..").toStringArray(), is(new String[] {"a", "b", "c", "d", "e"}));
    }

    @Test
    public void shouldResolveRelativePaths() {
        assertThat(validPath.resolve("../../../../../.."), is(sameInstance(Path.ROOT)));
        assertThat(validPath.resolve("../.."), is(validPath.getAncestor().getAncestor()));
        assertThat(validPath.resolve("../..").toStringArray(), is(new String[] {"a", "b", "c", "d"}));
        assertThat(validPath.resolve("../x/../y/../z/.."), is(validPath.getAncestor()));
        assertThat(validPath.resolve("../x/../y/../z/..").toStringArray(), is(new String[] {"a", "b", "c", "d", "e"}));
        assertThat(validPath.resolve("../x").toStringArray(), is(new String[] {"a", "b", "c", "d", "e", "x"}));
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotResolveRelativePathWithMoreParentReferencesThanThePath() {
        validPath.resolve("../../../../../../..");
    }

    @Test
    public void shouldResolveNonAbsolutePaths() {
        validPath = new Path("a/b/c");
        assertThat(validPath.toStringArray(), is(new String[] {"a", "b", "c"}));
    }

    @Test
    public void shouldResolveAbsolutePathsWithIndexes() {
        validPath = new Path("/a/b[3]/c");
        assertThat(validPath.toStringArray(), is(new String[] {"a", "b[3]", "c"}));

        validPath = new Path("/a/b[3]/c[4]");
        assertThat(validPath.toStringArray(), is(new String[] {"a", "b[3]", "c[4]"}));
    }

    @Test
    public void shouldResolveNonAbsolutePathsWithIndexes() {
        validPath = new Path("a/b[3]/c");
        assertThat(validPath.toStringArray(), is(new String[] {"a", "b[3]", "c"}));

        validPath = new Path("a/b[3]/c[4]");
        assertThat(validPath.toStringArray(), is(new String[] {"a", "b[3]", "c[4]"}));
    }

    @Test
    public void shouldReturnRootInstanceForParentOfPathWithOneSegment() {
        assertThat(new Path("/a").getAncestor(), is(sameInstance(Path.ROOT)));
    }

    @Test
    public void shouldNotConsiderPathToBeAncestorOfItself() {
        assertThat(validPath.isAncestorOf(validPath), is(false));
        assertThat(validPath.isAncestorOf(validPath.clone()), is(false));
    }

    @Test
    public void shouldNotConsiderPathToBeDecendantOfItself() {
        assertThat(validPath.isDecendantOf(validPath), is(false));
        assertThat(validPath.isDecendantOf(validPath.clone()), is(false));
    }

    @Test
    public void shouldConsiderParentPathTransitivelyToAllBeAncestors() {
        Path path = validPath;
        while (!path.isRoot()) {
            Path parent = path.getAncestor();
            // Check ancestry ...
            assertThat(parent.isAncestorOf(validPath), is(true));
            assertThat(parent.isAncestorOf(path), is(true));
            assertThat(path.isAncestorOf(parent), is(false));
            assertThat(validPath.isAncestorOf(parent), is(false));

            assertThat(validPath.isDecendantOf(parent), is(true));
            assertThat(path.isDecendantOf(parent), is(true));
            assertThat(parent.isDecendantOf(validPath), is(false));
            assertThat(parent.isDecendantOf(path), is(false));

            // Check whether the parent is the root ...
            if (parent.size() == 0) {
                assertThat(parent, is(sameInstance(Path.ROOT)));
                assertThat(parent.getAncestor(), is(sameInstance(Path.ROOT)));
            }
            assertThat(parent.size() + 1, is(path.size()));
            path = parent;
        }
    }

    @Test
    public void shouldReturnSameInstanceWhenAppendingNullSegment() {
        assertThat(validPath.append((String)null), is(sameInstance(validPath)));
    }

    @Test( expected = InvalidPathException.class )
    public void shouldFailWhenAppendingBlankSegment() {
        validPath.append("  \t  ");
    }

    @Test( expected = InvalidPathException.class )
    public void shouldFailWhenAppendingEmptySegment() {
        validPath.append("");
    }

    @Test( expected = InvalidPathException.class )
    public void shouldFailWhenAppendingBlankSegmentWithMultipleSegments() {
        validPath.append("a", "b", "  \t  ");
    }

    @Test( expected = InvalidPathException.class )
    public void shouldFailWhenAppendingEmptySegmentWithMultipleSegments() {
        validPath.append("a", "b", "");
    }

    @Test
    public void shouldReturnEquivalentPathsWhetherAppendingMultipleSegmentsOrAppendingMultipleTimes() {
        assertThat(validPath.append("x", "y", "z"), is(validPath.append("x").append("y").append("z")));
    }

    @Test
    public void shouldDecodeSegmentsWhenAppendingWithSpecificTextEncoder() {
        assertThat(validPath.append("^/ ", Path.JSR283_ENCODER).getString(), is(validPath.getString() + "/^\uF02F "));
        assertThat(validPath.append("^/", Path.URL_ENCODER).getString(Path.URL_ENCODER), is(validPath.getString(Path.URL_ENCODER) + "/%5e%2f"));
    }

    @Test( expected = InvalidPathException.class )
    public void shouldNotDecodeSegmentsWhenAppendingWithoutSpecificTextEncoder() {
        validPath.append("^/ ");
    }

    @Test
    public void shouldOrderPathsCorrectly() {
        List<Path> paths = new ArrayList<Path>();
        paths.add(new Path("/a"));
        paths.add(new Path("/a/b"));
        paths.add(new Path("/a/b/alpha"));
        paths.add(new Path("/a/b/beta"));
        paths.add(new Path("/a/b/jcr:mixinTypes"));
        paths.add(new Path("/a/b/jcr:name"));
        paths.add(new Path("/a/b/jcr:primaryType"));
        paths.add(new Path("/a/c[1]"));
        paths.add(new Path("/a/c[1]/alpha"));
        paths.add(new Path("/a/c[1]/beta"));
        paths.add(new Path("/a/c[1]/jcr:mixinTypes"));
        paths.add(new Path("/a/c[1]/jcr:name"));
        paths.add(new Path("/a/c[1]/jcr:primaryType"));
        paths.add(new Path("/a/c[2]"));
        paths.add(new Path("/a/c[2]/alpha"));
        paths.add(new Path("/a/c[2]/beta"));
        paths.add(new Path("/a/c[2]/jcr:mixinTypes"));
        paths.add(new Path("/a/c[2]/jcr:name"));
        paths.add(new Path("/a/c[2]/jcr:primaryType"));

        // Randomize the list of paths, so we have something to sort ...
        List<Path> randomizedPaths = new ArrayList<Path>(paths);
        Collections.shuffle(randomizedPaths);
        assertThat(randomizedPaths, is(not(paths)));

        // Sort ...
        Collections.sort(randomizedPaths);
        assertThat(randomizedPaths, is(paths));
    }

    @Test
    public void shouldGetRelativePathToAncestorPath() {
        assertThat(new Path("/a/b/c/d").relativeTo(new Path("/a/b/c")), is(new Path("d")));
        assertThat(new Path("/a/b/c/d/e").relativeTo(new Path("/a/b/c")), is(new Path("d/e")));
    }

    @Test
    public void shouldGetRelativePathToDecendantPath() {
        assertThat(new Path("/a/b/c").relativeTo(new Path("/a/b/c/d")), is(new Path("..")));
        assertThat(new Path("/a/b/c").relativeTo(new Path("/a/b/c/d/e")), is(new Path("../..")));
    }

    @Test
    public void shouldGetRelativePathToPathWithCommonAncestor() {
        assertThat(new Path("/a/b/c/d").relativeTo(new Path("/a/b/e/f")), is(new Path("../../c/d")));
    }

    @Test
    public void shouldGetRelativePathToPathWithNoCommonAncestor() {
        assertThat(new Path("/a/b/c").relativeTo(new Path("/e/f")), is(new Path("../../a/b/c")));
        assertThat(new Path("/e/f/").relativeTo(new Path("/a/b/c")), is(new Path("../../../e/f")));
    }

}
