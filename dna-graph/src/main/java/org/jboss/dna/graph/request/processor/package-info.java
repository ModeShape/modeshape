/**
 * This portion of the JBoss DNA Graph API defines the {@link RequestProcessor processor} for {@link org.jboss.dna.graph.request.Request requests}.
 * Simple enough, it defines methods that handle the processing of each kind of request
 * (for example, {@link RequestProcessor#process(org.jboss.dna.graph.request.ReadNodeRequest)}).
 * Usually, an implementation will inherit default implementations for some of these methods, but will override
 * others (or provide implementations for the abstract methods).
 * <p>
 * The design of the processor is to have a separate <code>process(...)</code> methods that take as their only parameter
 * a particular kind of {@link org.jboss.dna.graph.request.Request}.  Since the code to process each kind of request
 * is likely to be different, this helps to separate all the different processing code.
 * </p>
 * <p>The design also makes it possible to easily inherit or override <code>process(...)</code> implementations.
 * In fact, the {@link RequestProcessor} abstract class provides a number of default implementations that are
 * pretty good.  Sure, the default implementations may not the fastest, but it allows you to implement the minimum
 * number of methods and have a complete processor.  And should you find that the performance is not good enough
 * (which you'll verify by actually measuring performance, right?), then simply override the method in question
 * with an implementation that is more efficient.  In other words, start simple and add complexity only when needed.
 * </p>
 * <p>
 * This design has a great benefit, though: backward compability.  Let's imagine that you're using a particular release
 * of JBoss DNA, and have written a {@link org.jboss.dna.graph.connector.RepositoryConnection connector} that uses
 * your own {@link RequestProcessor} subclass.  The next release of JBoss DNA might include additional request types
 * and provide default implementations for the corresponding <code>process(NewRequestType)</code> method, and your 
 * {@link RequestProcessor} subclass (written against an earlier release) will automatically work with the next release.
 * Plus, your connector will inherit the new functionality with zero effort on your part.
 * </p>
 */

package org.jboss.dna.graph.request.processor;

