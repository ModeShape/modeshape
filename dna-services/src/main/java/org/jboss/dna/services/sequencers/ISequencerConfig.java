/*
 *
 */
package org.jboss.dna.services.sequencers;

import java.util.List;
import org.jboss.dna.maven.MavenId;

/**
 * @author Randall Hauch
 */
public interface ISequencerConfig extends Comparable<ISequencerConfig> {

    String getName();

    String getDescription();

    String getSequencerClassname();

    List<MavenId> getSequencerClasspath();

}
