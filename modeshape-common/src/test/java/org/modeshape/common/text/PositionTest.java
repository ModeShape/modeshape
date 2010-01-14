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
package org.modeshape.common.text;

import static org.junit.Assert.assertThat;

import static org.hamcrest.core.Is.is;

import org.junit.Test;


public class PositionTest {
	
	private int combinedIndex(int firstIndex, int secondIndex) {
		Position first = new Position(firstIndex, 1, 0);
		Position second = new Position(secondIndex, 1, 0);
		
		int firstPlusSecond = first.add(second).getIndexInContent();
		int secondPlusFirst = second.add(first).getIndexInContent();
		
		assertThat(firstPlusSecond, is(secondPlusFirst));
		
		return firstPlusSecond;
	}
	
	@Test
	public void shouldAddNoContentPositionToValidPosition() {
		// -1 to >=0
		assertThat(combinedIndex(-1, 0), is(0));
		assertThat(combinedIndex(-1, 1), is(1));
		assertThat(combinedIndex(-1, 10), is(10));
	}
	
	@Test
	public void shouldAddValidPositionToNoContentPosition() {
		// >= 0 to -1
		assertThat(combinedIndex(0, -1), is(0));
		assertThat(combinedIndex(1, -1), is(1));
		assertThat(combinedIndex(10, -1), is(10));
	}
	
	@Test
	public void shouldAddValidPositionToValidPosition() {
		// positive to positive
		assertThat(combinedIndex(1, 1), is(2));
		assertThat(combinedIndex(10, 1), is(11));
		assertThat(combinedIndex(1, 10), is(11));
		assertThat(combinedIndex(10, 10), is(20));
	}
	
	@Test
	public void shouldAddStartingPositionToStartingPosition() {
		// 0 to 0
		assertThat(combinedIndex(0, 0), is(0));
	}
	
	@Test
	public void shouldAddNoContentPositionToNoContentPosition() {
		// -1 to -1
		assertThat(combinedIndex(-1, -1), is(-1));
		assertThat(combinedIndex(-10, -1), is(-1));
		assertThat(combinedIndex(-1, -10), is(-1));
	}
}
