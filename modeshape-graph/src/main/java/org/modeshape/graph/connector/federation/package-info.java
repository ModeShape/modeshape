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
 * ModeShape provides a federated connector that is able to access repository content from multiple external systems
 * and make that content look like it exists in a single unified repository.  Like other connectors, a 
 * {@link org.modeshape.graph.connector.RepositorySource} implementation is provided, called {@link FederatedRepositorySource},
 * that can be set up and configured to be used by a {@link org.modeshape.graph.Graph} or a JCR repository. 
 * 
 * <h3>Projections</h3>
 * <p>
 * Each federated repository source provides a unified repository consisting of information that is dynamically federated
 * from multiple other RepositorySource instances. The connector is configured with a number of <strong>projections</strong>
 * that each describe where in the unified repository the federated connector should place the content from another source.
 * Projections consist of the name of the source containing the content and a number of <strong>rules</strong> that
 * define the path mappings, where each rule is defined as a string with this format:
 * </p>
 * <pre>
 *       pathInFederatedRepository => pathInSourceRepository
 * </pre>
 * <p>
 * Here, the <code>pathInFederatedRepository</code> is the string representation of the path in the unified
 * (or federated) repository, and <code>pathInSourceRepository</code> is the string representation of the path of the
 * actual content in the underlying source.  For example:
 * </p>
 * <pre>
 *       / => /
 * </pre>
 * <p>
 * is a trivial rule that states that all of the content in the underlying source should be mapped into the unified
 * repository such that the locations are the same.  Therefore, a node at <code>/a/b/c</code> in the source would
 * appear in the unified repository at <code>/a/b/c</code>.  This is called a <strong>mirror projection</strong>,
 * since the unified repository mirrors the underlying source repository.
 * </p>
 * <p>
 * Another example is an <strong>offset projection</strong>, which is similar to the mirror projection except that
 * the federated path includes an offset not found in the source:
 * </p>
 * <pre>
 *       /alpha/beta => /
 * </pre>
 * <p>
 * Here, a node at <code>/a/b/c</code> in the source would actually appear in the unified repository at 
 * <code>/alpha/beta/a/b/c</code>.  The offset path (<code>/alpha/beta</code> in this example) can have 1 or more segments.
 * (If there are no segments, then it reduces to a mirror projection.)
 * </p>
 * <p>
 * Often a rule will map a path in one source into another path in the unified source:
 * </p>
 * <pre>
 *       /alpha/beta => /foo/bar
 * </pre>
 * <p>
 * Here, the content at <code>/foo/bar</code> is projected in the unified repository under <code>/alpha/beta</code>,
 * meaning that the <code>/foo/bar</code> prefix never even appears in the unified repository.  So the node at
 * <code>/foo/bar/baz/raz</code> would appear in the unified repository at <code>/alpha/beta/baz/raz</code>.  Again,
 * the size of the two paths in the rule don't matter.
 * </p>
 * <h3>Multiple Projections</h3>
 * <p>
 * Federated repositories that use a single projection are useful, but they aren't as interesting or powerful as
 * those that use multiple projections.  Consider a federated repository that is defined by two projections:
 * </p>
 * <pre>
 *       / => /                         for source "S1"
 *       /alpha => /foo/bar             for source "S2"
 * </pre>
 * <p>
 * And consider that S1 contains the following structure:
 * </p>
 * <pre>
 *      +- a
 *      |  +- i
 *      |  +- j
 *      +- b
 *         +- k
 *         +- m
 *         +- n
 * </pre>
 * and S2 contains the following:
 * <pre>
 *      +- foo
 *         +- bar
 *         |  +- baz
 *         |  |  +- taz
 *         |  |  +- zaz
 *         |  +- raz
 *         +- bum
 *            +- bot
 * </pre>
 * <p>
 * The unified repository would then have this structure:
 * </p>
 * <pre>
 *      +- a
 *      |  +- i
 *      |  +- j
 *      +- b
 *      |  +- k
 *      |  +- m
 *      |  +- n
 *      +- alpha
 *         +- baz
 *            +- taz
 *            |  +- zaz
 *            +- raz
 * </pre>
 * <p>
 * Note how the <code>/foo/bum</code> branch does not even appear in the unified repository, since it is outside of the
 * branch being projected. Also, the <code>/alpha</code> node doesn't exist in S1 or S2; it's what is called a
 * <strong>placeholder</strong> node that exists purely so that the nodes below it have a place to exist.
 * Placeholders are somewhat special: they allow any structure below them (including other placeholder nodes or real
 * projected nodes), but they cannot be modified.
 * </p>
 * <p>
 * Even more interesting are cases that involve more projections.  Consider a federated repository that contains
 * information about different kinds of automobiles, aircraft, and spacecraft, except that the information
 * about each kind of vehicle exists in a different source (and possibly a different <i>kind</i> of source, such as
 * a database, or file, or web service).
 * <p>
 * First, the sources.  The "Cars" source contains the following structure:
 * </p>
 * <pre>
 *      +- Cars
 *         +- Hybrid
 *         |  +- Toyota Prius
 *         |  +- Toyota Highlander
 *         |  +- Nissan Altima
 *         +- Sports
 *         |  +- Aston Martin DB9
 *         |  +- Infinity G37
 *         +- Luxury
 *         |  +- Cadillac DTS
 *         |  +- Bentley Continental
 *         |  +- Lexus IS350
 *         +- Utility
 *            +- Land Rover LR2
 *            +- Land Rover LR3
 *            +- Hummer H3
 *            +- Ford F-150
 * </pre>
 * <p>
 * The "Aircraft" source contains the following structure:
 * </p>
 * <pre>
 *      +- Aviation
 *         +- Business
 *         |  +- Gulfstream V
 *         |  +- Learjet 45
 *         +- Commercial
 *         |  +- Boeing 777
 *         |  +- Boeing 767
 *         |  +- Boeing 787
 *         |  +- Boeing 757
 *         |  +- Airbus A380
 *         |  +- Airbus A340
 *         |  +- Airbus A310
 *         |  +- Embraer RJ-175
 *         +- Vintage
 *         |  +- Fokker Trimotor
 *         |  +- P-38 Lightning
 *         |  +- A6M Zero
 *         |  +- Bf 109
 *         |  +- Wright Flyer
 *         +- Homebuilt
 *            +- Long-EZ
 *            +- Cirrus VK-30
 *            +- Van's RV-4
 * </pre>
 * <p>
 * Finally, our "Spacecraft" source contains the following structure:
 * </p>
 * <pre>
 *      +- Space Vehicles
 *         +- Manned
 *         |  +- Space Shuttle
 *         |  +- Soyuz
 *         |  +- Skylab
 *         |  +- ISS
 *         +- Unmanned
 *         |  +- Sputnik
 *         |  +- Explorer
 *         |  +- Vanguard
 *         |  +- Pioneer
 *         |  +- Marsnik
 *         |  +- Mariner
 *         |  +- Mars Pathfinder
 *         |  +- Mars Observer
 *         |  +- Mars Polar Lander
 *         +- Launch Vehicles
 *         |  +- Saturn V
 *         |  +- Aries
 *         |  +- Delta
 *         |  +- Delta II
 *         |  +- Orion
 *         +- X-Prize
 *            +- SpaceShipOne
 *            +- WildFire
 *            +- Spirit of Liberty
 * </pre>
 * <p>
 * So, we can define our unified "Vehicles" source with the following projections:
 * </p>
 * <pre>
 *       /Vehicles => /                                  for source "Cars"
 *       /Vehicles/Aircraft => /Aviation                 for source "Aircraft"
 *       /Vehicles/Spacecraft => /Space Vehicles         for source "Cars"
 * </pre>
 * <p>
 * The result is a unified repository with the following structure:
 * </p>
 * <pre>
 *      +- Vehicles
 *         +- Cars
 *         |  +- Hybrid
 *         |   |  +- Toyota Prius
 *         |   |  +- Toyota Highlander
 *         |   |  +- Nissan Altima
 *         |   +- Sports
 *         |   |  +- Aston Martin DB9
 *         |   |  +- Infinity G37
 *         |   +- Luxury
 *         |   |  +- Cadillac DTS
 *         |   |  +- Bentley Continental
 *         |  +- Lexus IS350
 *         |  +- Utility
 *         |     +- Land Rover LR2
 *         |     +- Land Rover LR3
 *         |     +- Hummer H3
 *         |     +- Ford F-150
 *         +- Aircraft
 *         |   +- Business
 *         |   |  +- Gulfstream V
 *         |   |  +- Learjet 45
 *         |   +- Commercial
 *         |   |  +- Boeing 777
 *         |   |  +- Boeing 767
 *         |   |  +- Boeing 787
 *         |   |  +- Boeing 757
 *         |   |  +- Airbus A380
 *         |   |  +- Airbus A340
 *         |   |  +- Airbus A310
 *         |   |  +- Embraer RJ-175
 *         |   +- Vintage
 *         |   |  +- Fokker Trimotor
 *         |   |  +- P-38 Lightning
 *         |   |  +- A6M Zero
 *         |   |  +- Bf 109
 *         |   |  +- Wright Flyer
 *         |   +- Homebuilt
 *         |      +- Long-EZ
 *         |      +- Cirrus VK-30
 *         |      +- Van's RV-4
 *         +- Spacecraft
 *            +- Manned
 *            |  +- Space Shuttle
 *            |  +- Soyuz
 *            |  +- Skylab
 *            |  +- ISS
 *            +- Unmanned
 *            |  +- Sputnik
 *            |  +- Explorer
 *            |  +- Vanguard
 *            |  +- Pioneer
 *            |  +- Marsnik
 *            |  +- Mariner
 *            |  +- Mars Pathfinder
 *            |  +- Mars Observer
 *            |  +- Mars Polar Lander
 *            +- Launch Vehicles
 *            |  +- Saturn V
 *            |  +- Aries
 *            |  +- Delta
 *            |  +- Delta II
 *            |  +- Orion
 *            +- X-Prize
 *               +- SpaceShipOne
 *               +- WildFire
 *               +- Spirit of Liberty
 * </pre>
 * <p>
 * Other combinations are of course possible.
 * </p>
 */
package org.modeshape.graph.connector.federation;

