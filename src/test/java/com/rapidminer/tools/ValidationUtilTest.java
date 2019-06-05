/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.tools.ValidationUtil;


/**
 * Tests for {@link ValidationUtil}
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ValidationUtilTest {

	private static List<String> stringValues;
	private static Map<String, List<String>> simpleChain;
	private static List<String> chainValues;
	private static Map<String, List<String>> simpleLoop;
	private static Map<String, List<String>> tree;
	private static List<String> treeValues;
	private static Map<String, List<String>> graph;
	private static List<String> graphValues;

	@BeforeClass
	public static void setup() {
		stringValues = Arrays.asList("a", "b", "c", "d", "e", "f");

		// a -> b -> c -> d
		simpleChain = new HashMap<>(3);
		simpleChain.put(stringValues.get(0), Collections.singletonList(stringValues.get(1)));
		simpleChain.put(stringValues.get(1), Collections.singletonList(stringValues.get(2)));
		simpleChain.put(stringValues.get(2), Collections.singletonList(stringValues.get(3)));
		chainValues = IntStream.of(3, 4, 5, 2, 1, 0).mapToObj(stringValues::get).collect(Collectors.toList());

		// a -> b -> c -> a
		simpleLoop = new HashMap<>(3);
		simpleLoop.put(stringValues.get(0), Collections.singletonList(stringValues.get(1)));
		simpleLoop.put(stringValues.get(1), Collections.singletonList(stringValues.get(2)));
		simpleLoop.put(stringValues.get(2), Collections.singletonList(stringValues.get(0)));

		// a -> b -> d -> e
		//  \-> c
		tree = new HashMap<>(3);
		tree.put(stringValues.get(0), Arrays.asList(stringValues.get(1), stringValues.get(2)));
		tree.put(stringValues.get(1), Collections.singletonList(stringValues.get(3)));
		tree.put(stringValues.get(3), Collections.singletonList(stringValues.get(4)));
		treeValues = IntStream.of(2, 4, 5, 3, 1, 0).mapToObj(stringValues::get).collect(Collectors.toList());

		// a -> b -> d
		// c -/  \-> e
		graph = new HashMap<>(3);
		graph.put(stringValues.get(1), Arrays.asList(stringValues.get(3), stringValues.get(4)));
		graph.put(stringValues.get(0), Collections.singletonList(stringValues.get(1)));
		graph.put(stringValues.get(2), Collections.singletonList(stringValues.get(1)));
		graphValues = IntStream.of(3, 4, 5, 1, 0, 2).mapToObj(stringValues::get).collect(Collectors.toList());
	}

	@AfterClass
	public static void tearDown() {
		stringValues = null;
		simpleChain = simpleLoop = tree = graph = null;
	}

	@Test
	public void testSimpleChainDependency() {
		testListSort(simpleChain, chainValues);
		testListSortSuppressed(simpleChain, chainValues);
	}

	@Test
	public void testSimpleLoopDependency() {
		testListSort(simpleLoop, null);
		testListSortSuppressed(simpleLoop, Collections.emptyList());
	}

	@Test
	public void testTreeDependency() {
		testListSort(tree, treeValues);
		testListSortSuppressed(tree, treeValues);
	}

	@Test
	public void testGraphDependency() {
		testListSort(graph, graphValues);
		testListSortSuppressed(graph, graphValues);
	}

	/**
	 * Run {@link ValidationUtil#dependencySortEmptyListForLoops(Function, Collection)} and check results;
	 * <p>
	 * If the expected results are not {@code null}, expected and calculated results are compared.
	 * If an {@link IllegalArgumentException} is thrown in this case, the check will fail.
	 * <p>
	 * If the expected results are {@code null}, but there is no exception thrown, the check will fail.
	 */
	private void testListSort(Map<String, List<String>> dependency, List<String> expectedResult) {
		List<String> sortedValues = null;
		try {
			sortedValues = ValidationUtil.dependencySortNoLoops(
					k -> new HashSet<>(dependency.getOrDefault(k, Collections.emptyList())), stringValues);
			if (expectedResult != null) {
				assertEquals(expectedResult, sortedValues);
			} else {
				fail("Expected IllegalArgumentException because of looped dependencies");
			}
		} catch (IllegalArgumentException iae) {
			if (expectedResult != null) {
				fail("Unexpected IllegalArgumentException for " + dependency);
			}
		}
	}

	/** Run {@link ValidationUtil#dependencySortEmptyListForLoops(Function, Collection)} and compare result */
	private void testListSortSuppressed(Map<String, List<String>> dependency, List<String> expectedResult) {
		List<String> sortedValues = null;
		sortedValues = ValidationUtil.dependencySortEmptyListForLoops(
				k -> new HashSet<>(dependency.getOrDefault(k, Collections.emptyList())), stringValues);
		assertEquals(expectedResult, sortedValues);
	}
}