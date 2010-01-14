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
/**
 * This portion of the ModeShape Graph API defines the {@link RequestProcessor processor} for {@link org.modeshape.graph.request.Request requests}.
 * Simple enough, it defines methods that handle the processing of each kind of request
 * (for example, {@link RequestProcessor#process(org.modeshape.graph.request.ReadNodeRequest)}).
 * Usually, an implementation will inherit default implementations for some of these methods, but will override
 * others (or provide implementations for the abstract methods).
 * <p>
 * The design of the processor is to have a separate <code>process(...)</code> methods that take as their only parameter
 * a particular kind of {@link org.modeshape.graph.request.Request}.  Since the code to process each kind of request
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
 * of ModeShape, and have written a {@link org.modeshape.graph.connector.RepositoryConnection connector} that uses
 * your own {@link RequestProcessor} subclass.  The next release of ModeShape might include additional request types
 * and provide default implementations for the corresponding <code>process(NewRequestType)</code> method, and your 
 * {@link RequestProcessor} subclass (written against an earlier release) will automatically work with the next release.
 * Plus, your connector will inherit the new functionality with zero effort on your part.
 * </p>
 */

package org.modeshape.graph.request.processor;

