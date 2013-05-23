package org.modeshape.common.collection;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.modeshape.common.collection.IsIteratorContaining.hasItems;

public class CollectionsTest {

	@Test
	public void shouldConcatenateIterables() throws Exception {
		Iterable<String> a = Arrays.asList("a", "b", "c");
		Iterable<String> b = Arrays.asList("1", "2", "3");

		final Iterable<String> concatIterable = Collections.concat(a, b);

		List<String> actual = new ArrayList<String>();

		final Iterator<String> iterator = concatIterable.iterator();

		assertThat(iterator, hasItems("a", "b", "c", "1", "2", "3"));
	}

	public void shouldConcatenateIterators() throws Exception {

	}
}
