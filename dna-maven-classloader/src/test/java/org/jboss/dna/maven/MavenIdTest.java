/*
 *
 */
package org.jboss.dna.maven;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class MavenIdTest {

    private String validGroupId;
    private String validArtifactId;
    private String validClassifier;
    private String validVersion;
    private String validArtifactIdToString;
    private String validArtifactIdWithNullClassifierToString;
    private MavenId validId;
    private MavenId validIdWithNullClassifier;

    @Before
    public void beforeEach() throws Exception {
        this.validGroupId = "org.jboss.dna";
        this.validArtifactId = "jboss-dna-core";
        this.validClassifier = "jdk1.4";
        this.validVersion = "1.0";
        this.validId = new MavenId(this.validGroupId, this.validArtifactId, this.validVersion, this.validClassifier);
        this.validIdWithNullClassifier = new MavenId(this.validGroupId, this.validArtifactId, this.validVersion, null);
        this.validArtifactIdToString = this.validGroupId + ":" + this.validArtifactId + ":" + this.validVersion + ":" + this.validClassifier;
        this.validArtifactIdWithNullClassifierToString = this.validGroupId + ":" + this.validArtifactId + ":" + this.validVersion + ":";
    }

    @Test
    public void shouldParseValidVersionStringIntoComponents() {
        assertArrayEquals(new Object[] {1, 2}, MavenId.getVersionComponents("1.2"));
        assertArrayEquals(new Object[] {1, 2, 3, 4, 5, 6, 7, 8}, MavenId.getVersionComponents("1.2.3.4.5.6.7.8"));
        assertArrayEquals(new Object[] {1, 0, "SNAPSHOT"}, MavenId.getVersionComponents("1.0-SNAPSHOT"));
        assertArrayEquals(new Object[] {1, 0, "SNAPSHOT"}, MavenId.getVersionComponents("1.0,SNAPSHOT"));
        assertArrayEquals(new Object[] {1, 0, "SNAPSHOT"}, MavenId.getVersionComponents("1.0/SNAPSHOT"));
        assertArrayEquals(new Object[] {1, 0, "SNAPSHOT"}, MavenId.getVersionComponents("1.0-SNAPSHOT"));
    }

    @Test
    public void shouldParseEmptyOrNullVersionStringIntoEmptyComponents() {
        assertArrayEquals(new Object[] {}, MavenId.getVersionComponents(null));
        assertArrayEquals(new Object[] {}, MavenId.getVersionComponents(""));
        assertArrayEquals(new Object[] {}, MavenId.getVersionComponents("   "));
    }

    @Test
    public void shouldParseVersionStringWithEmbeddedWhitespaceIntoEmptyComponents() {
        assertArrayEquals(new Object[] {1, 2, "SNAPSHOT"}, MavenId.getVersionComponents("  1.2-SNAPSHOT  "));
        assertArrayEquals(new Object[] {1, 2, "SNAPSHOT"}, MavenId.getVersionComponents("  1  . 2  -  SNAPSHOT  "));
        assertArrayEquals(new Object[] {1, 2, "SNAP SHOT"}, MavenId.getVersionComponents("  1  . 2  -  SNAP SHOT  "));
    }

    @Test
    public void shouldCreateInstanceWithValidArguments() {
        new MavenId(this.validGroupId, this.validArtifactId, this.validVersion);
        new MavenId(this.validGroupId, this.validArtifactId, this.validVersion, this.validClassifier);
    }

    @Test
    public void shouldCreateInstanceWithNullOrEmptyVersion() {
        assertThat(new MavenId(this.validGroupId, this.validArtifactId, null, this.validClassifier), is(notNullValue()));
        assertThat(new MavenId(this.validGroupId, this.validArtifactId, "", this.validClassifier), is(notNullValue()));
        assertThat(new MavenId(this.validGroupId, this.validArtifactId, "   ", this.validClassifier), is(notNullValue()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateInstanceWithNullGroupId() {
        new MavenId(null, this.validArtifactId, this.validVersion, this.validClassifier);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateInstanceWithNullArtifactId() {
        new MavenId(this.validGroupId, null, this.validVersion, this.validClassifier);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateInstanceWithEmptyGroupId() {
        new MavenId("  ", this.validArtifactId, this.validVersion, this.validClassifier);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateInstanceWithEmptyArtifactId() {
        new MavenId(this.validGroupId, "  ", this.validVersion, this.validClassifier);
    }

    @Test
    public void shouldCompareToSelfAsSame() {
        assertThat(this.validId.compareTo(this.validId), is(0));
        assertThat(this.validIdWithNullClassifier.compareTo(this.validIdWithNullClassifier), is(0));
    }

    @Test
    public void shouldEqualSelf() {
        assertThat(this.validId.equals(this.validId), is(true));
        assertThat(this.validIdWithNullClassifier.equals(this.validIdWithNullClassifier), is(true));
    }

    @Test
    public void shouldHaveToStringThatIsCombinationOfAllMembers() {
        assertThat(this.validId.toString(), is(this.validArtifactIdToString));
        assertThat(this.validIdWithNullClassifier.toString(), is(this.validArtifactIdWithNullClassifierToString));
    }

    @Test
    public void shouldHaveRepeatableHashCode() {
        int hc = this.validId.hashCode();
        for (int i = 0; i != 10; ++i) {
            assertThat(this.validId.hashCode(), is(hc));
        }
    }

    @Test
    public void shouldAlwaysEqualAnyVersion() {
        MavenId that = this.validId;
        MavenId any = new MavenId(that.getGroupId(), that.getArtifactId(), null, that.getClassifier());
        assertThat(any.equals(that), is(true));
    }
}
