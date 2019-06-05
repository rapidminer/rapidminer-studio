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

import static com.rapidminer.tools.ListenerTools.informAllAndThrow;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import com.rapidminer.operator.OperatorException;


/**
 * Test class for {@link ListenerTools}
 *
 * @author Jan Czogalla
 * @since 9.3.0
 */
public class ListenerToolsTest {

	/** Simple struct to describe test cases */
	private static class TestCase {

		private String description;
		private int[] creators;
		private Class<? extends Throwable> eClass;
		private String expectedMessage;
		private int expectedThrowables;
		private int expectedSuppressed;
		private TestCase(String description, int[] creators, Class<? extends Throwable> eClass, String expectedMessage, int expectedThrowables, int expectedSuppressed) {
			this.description = description;
			this.creators = creators;
			this.eClass = eClass;
			this.expectedMessage = expectedMessage;
			this.expectedThrowables = expectedThrowables;
			this.expectedSuppressed = expectedSuppressed;
		}
	}

	/** Simple interface that represents a {@link java.util.function.BiFunction} with {@link Throwable} */
	private interface Tester {
		List<Throwable> test(int[] creators, Class<? extends Throwable> eClass) throws Throwable;
	}

	/** Throws a given exception; does nothing on {@code null} values */
	private static final ConsumerWithThrowable<Throwable, ? extends Throwable> THROWER = e -> {if (e != null) throw e;};
	/** Indicates non-throwing for {@link #THROWABLE_CREATORS} */
	private static final int NONE = 0;
	/** Indicates {@link RuntimeException}-throwing for {@link #THROWABLE_CREATORS} */
	private static final int RUNTIME = 1;
	/** Indicates {@link OperatorException}-throwing for {@link #THROWABLE_CREATORS} */
	private static final int OPERATOR = 2;
	/** Exception creators; the first one creates {@code null}*/
	@SuppressWarnings("unchecked")
	private static final Function<String, Throwable>[] THROWABLE_CREATORS = new Function[]{
			m -> null, m -> new RuntimeException(m.toString()), m -> new OperatorException(m.toString())};
	/** Maps an int to <em>a-z</em>*/
	private static final IntFunction<String> MESSAGES = i -> "" + (char) ('a' + i);

	private static final TestCase[] TEST_CASES = new TestCase[]{
			new TestCase("no exceptions (Throwable)", new int[]{NONE, NONE, NONE}, Throwable.class, null, 0, 0),
			new TestCase("no exceptions (Runtime)", new int[]{NONE, NONE, NONE}, RuntimeException.class, null, 0, 0),
			new TestCase("no exceptions (Operator)", new int[]{NONE, NONE, NONE}, OperatorException.class, null, 0, 0),
			new TestCase("only matching exceptions (Runtime)", new int[]{RUNTIME, RUNTIME, RUNTIME}, RuntimeException.class, "a", 0, 2),
			new TestCase("only matching exceptions (Operator)", new int[]{OPERATOR, OPERATOR, OPERATOR}, OperatorException.class, "a", 0, 2),
			new TestCase("no matching exceptions (Runtime)", new int[]{OPERATOR, OPERATOR, OPERATOR}, RuntimeException.class, null, 3, 0),
			new TestCase("no matching exceptions (Operator)", new int[]{RUNTIME, RUNTIME, RUNTIME}, OperatorException.class, null, 3, 0),
			new TestCase("only first matching (Runtime)", new int[]{RUNTIME, OPERATOR, OPERATOR}, RuntimeException.class, "a", 0, 0),
			new TestCase("only first matching (Operator)", new int[]{OPERATOR, RUNTIME, RUNTIME}, OperatorException.class, "a", 0, 0),
			new TestCase("only second matching (Runtime)", new int[]{OPERATOR, RUNTIME, OPERATOR}, RuntimeException.class, "b", 0, 0),
			new TestCase("only second matching (Operator)", new int[]{RUNTIME, OPERATOR, RUNTIME}, OperatorException.class, "b", 0, 0),
			new TestCase("only third matching (Runtime)", new int[]{OPERATOR, OPERATOR, RUNTIME}, RuntimeException.class, "c", 0, 0),
			new TestCase("only third matching (Operator)", new int[]{RUNTIME, RUNTIME, OPERATOR}, OperatorException.class, "c", 0, 0),
			new TestCase("only third not matching (Runtime)", new int[]{RUNTIME, RUNTIME, OPERATOR}, RuntimeException.class, "a", 0, 1),
			new TestCase("only third not matching (Operator)", new int[]{OPERATOR, OPERATOR, RUNTIME}, OperatorException.class, "a", 0, 1),
			new TestCase("only second not matching (Runtime)", new int[]{RUNTIME, OPERATOR, RUNTIME}, RuntimeException.class, "a", 0, 1),
			new TestCase("only second not matching (Operator)", new int[]{OPERATOR, RUNTIME, OPERATOR}, OperatorException.class, "a", 0, 1),
			new TestCase("only first not matching (Runtime)", new int[]{OPERATOR, RUNTIME, RUNTIME}, RuntimeException.class, "b", 0, 1),
			new TestCase("only first not matching (Operator)", new int[]{RUNTIME, OPERATOR, OPERATOR}, OperatorException.class, "b", 0, 1)
	};

	@Test
	public void testGeneralListeners() {
		test("general list", ListenerToolsTest::testWithClass, c -> false);
	}

	@Test
	public void testOperatorListeners() {
		test("operator exception list", ListenerToolsTest::testWithOperatorException,
				// only tests with expected error type OperatorException
				tc -> tc.eClass != OperatorException.class);
	}

	@Test
	public void testRuntimeListeners() {
		test("runtime exception list", ListenerToolsTest::testWithRuntimeException,
				// only tests with expected error type RuntimeException that can not throw other exceptions
				tc -> tc.eClass != RuntimeException.class || tc.expectedThrowables > 0);
	}

	@Test
	public void testListenersAndInitial() {
		test("list with initial call", (Tester) ListenerToolsTest::testWithInital,
				// only tests with expected error type OperatorException
				tc -> tc.eClass != OperatorException.class);
	}

	/** Test all test cases from {@link #TEST_CASES} with the given tester and skip those that should be ignored. */
	private static void test(String description, Tester tester, Predicate<TestCase> ignore) {
		LogService.getRoot().info("Testing " + description);
		for (TestCase tc : TEST_CASES) {
			if (ignore.test(tc)) {
				continue;
			}
			LogService.getRoot().info("Testing case: \"" + tc.description + "\"");
			List<Throwable> throwables;
			try {
				throwables = tester.test(tc.creators, tc.eClass);
				Assert.assertEquals("Unexpected number of unexpected exceptions (not of " + tc.eClass.getSimpleName() + ")",
						tc.expectedThrowables, throwables.size());
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable e) {
				if (tc.expectedMessage == null) {
					Assert.fail("Unexpected exception: " + e);
				}
				Assert.assertEquals("Unexpected exception message", tc.expectedMessage, e.getMessage());
				Assert.assertEquals("Incorrect number of suppressed exceptions", tc.expectedSuppressed, e.getSuppressed().length);
			}
		}
	}

	/** Tests the {@link ListenerTools#informAllAndThrow(Collection, ConsumerWithThrowable, Class) informAllAndThrow}
	 *  method for the throwing "listeners" as described by the creators array and exception class. */
	@SuppressWarnings("unchecked")
	private static <E extends Throwable> List<Throwable> testWithClass(int[] creators, Class<E> eClass) throws Throwable {
		return informAllAndThrow(getListeners(creators, 0), (ConsumerWithThrowable<Throwable, E>) THROWER, eClass);
	}

	/** Tests the {@link ListenerTools#informAllAndThrow(Collection, ConsumerWithThrowable) informAllAndThrow}
	 *  method for the throwing "listeners" as described by the creators array and exception class. */
	@SuppressWarnings("unchecked")
	private static <E extends Throwable> List<Throwable> testWithOperatorException(int[] creators, Class<E> eClass) throws Throwable {
		return informAllAndThrow(getListeners(creators, 0), (ConsumerWithThrowable<Throwable, OperatorException>) THROWER);
	}

	/** Tests the {@link ListenerTools#informAllAndThrow(Collection, java.util.function.Consumer) informAllAndThrow}
	 *  method for the throwing "listeners" as described by the creators array and exception class. */
	@SuppressWarnings("unchecked")
	private static List<Throwable> testWithRuntimeException(int[] creators, Class<? extends Throwable> eClass) throws Throwable{
		Consumer<Throwable> method = e -> {if (e instanceof RuntimeException) throw (RuntimeException) e;};
		return informAllAndThrow(getListeners(creators, 0), method);
	}

	/** Tests the {@link ListenerTools#informAllAndThrow(ConsumerWithThrowable, Collection, ConsumerWithThrowable) informAllAndThrow}
	 *  method for the throwing "listeners" as described by the creators array and exception class. */
	@SuppressWarnings("unchecked")
	private static <E extends Throwable> List<Throwable> testWithInital(int[] creators, Class<E> eClass) throws Throwable {
		Throwable initialSupplier = THROWABLE_CREATORS[creators[0]].apply(MESSAGES.apply(0));
		return informAllAndThrow(e -> ((ConsumerWithThrowable<Throwable, OperatorException>) THROWER).acceptWithException(initialSupplier),
				getListeners(creators, 1), (ConsumerWithThrowable<Throwable, OperatorException>) THROWER);
	}

	/** Create list of {@link Throwable} that are fed to the different methods to be tested */
	private static List<Throwable> getListeners(int[] creators, int offset) {
		return IntStream.range(offset, creators.length)
				.mapToObj(i -> THROWABLE_CREATORS[creators[i]].apply(MESSAGES.apply(i))).collect(Collectors.toList());
	}
}