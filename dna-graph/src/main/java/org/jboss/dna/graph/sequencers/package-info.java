/**
 * A sequencer in JBoss DNA is a component that is able to process information (usually the content of a file,
 * or a property value on a node) and recreate that information as a graph of structured content.  This package
 * defines the interfaces for the sequencing system.
 * <h3>StreamSequencer</h3>
 * <p>The {@link StreamSequencer} interface is a special form of sequencer that processes information coming
 * through an {@link java.io.InputStream}. Implementations are responsible for processing the content and generating
 * structured content using the supplied {@link SequencerOutput} interface.  Additional details about the information
 * being sequenced is available in the supplied {@link SequencerContext}.
 * </p>
 */

package org.jboss.dna.graph.sequencers;

