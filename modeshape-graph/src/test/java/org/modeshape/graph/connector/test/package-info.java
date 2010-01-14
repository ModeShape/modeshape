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
 * This package provides a set of unit tests that can be used to verify that a connector behaves correctly 
 * and in such a way that matches ModeShape's expectations.  These unit tests utilize JUnit 4 annotations, and are written
 * in a <a href="http://behaviour-driven.org/">Behavior-Driven Development</a> style where each test method
 * is named to describe the behavior being tested (e.g., "<code>shouldAlwaysBeAbleToReadRootNode()</code>").
 * This style helps to make more obvious the requirements and behavior that the test is verifying.
 * <p>
 * To use these unit tests, you simply extend the unit tests provided by this package.  But rather than write a
 * whole slew of test methods, your tests inherit all the test methods.  So, it's pretty easy to get a lot of
 * tests for almost no cost.
 * </p>
 * <p>
 * All unit tests (directly or indirectly) subclass {@link AbstractConnectorTest}, which does all the work of 
 * setting up and tearing down the connector, its environment, and common utility objects needed for many of
 * the tests.  This class defines two abstract methods that each of your test classes will have to implement:
 * <ul>
 *   <li>{@link AbstractConnectorTest#setUpSource() setUpSource()} - This method allows you to configure your
 *   {@link org.modeshape.graph.connector.RepositorySource}, and will be called once during the set up of each test method.</li>
 *   <li>{@link AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph) initializeContent()} - This method allows you to
 *   populate the repository with some predefined content, using the supplied {@link org.modeshape.graph.Graph graph}.
 *   This method will be called once during the set up of each test method (after {@link AbstractConnectorTest#setUpSource()}), 
 *   ensuring that each test method gets a fresh repository with content.
 *   If your connector is accessing an empty source, you should use this method to populate the source with some content.
 *   On the other hand, if your connector is accessing a system that already has its own content, you may still want
 *   to use this method to prepare the content or check that the content is what you're expecting.
 *   </li>
 * </ul>
 * The class also defines a number of utility methods that may be useful in different test methods.  These include (among others)
 * {@link AbstractConnectorTest#name(String)} to create a name from a string, {@link AbstractConnectorTest#path(String)} to
 * create a path from a string, {@link AbstractConnectorTest#segment(String)} to create a path segment from a string,
 * and several forms of {@link AbstractConnectorTest#createSubgraph(org.modeshape.graph.Graph, String, int, int, int, boolean, org.modeshape.common.statistic.Stopwatch, java.io.PrintStream, String) createSubgraph(...)}
 * that is useful for creating a highly-structured subgraph.
 * </p>
 * <p>
 * While you may choose to extend <code>AbstractConnectorTest</code>, you'll more likely want to extend one of the
 * other test classes that define test methods, including:
 * <ul>
 *  <li>{@link ReadableConnectorTest} - Reads the content of your source through a variety of methods, including
 *  {@link org.modeshape.graph.request.ReadNodeRequest reading one node at a time}, 
 *  {@link org.modeshape.graph.request.ReadBranchRequest reading branches}, 
 *  {@link org.modeshape.graph.request.ReadPropertyRequest reading one property at a time},
 *  {@link org.modeshape.graph.request.ReadAllPropertiesRequest reading all properties}, 
 *  {@link org.modeshape.graph.request.ReadAllChildrenRequest reading children}, etc.  These methods really just
 *  verify that the content read is consistent, and cannot verify that all the information is correct.
 *  </li>
 * </ul>
 * </p>
 */

package org.modeshape.graph.connector.test;
