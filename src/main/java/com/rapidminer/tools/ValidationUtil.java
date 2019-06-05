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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.connection.configuration.ConfigurationParameter;


/**
 * Utility class to check for different argument's validity. Instead of throwing {@link NullPointerException NullPointerExceptions}
 * like the {@link Objects} class often does, throws more comprehensible {@link IllegalArgumentException IllegalArgumentExceptions},
 * indicating the arguments name if given.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public final class ValidationUtil {

	private static final String DOT_AS_STRING = ".";

	/** Utility class; don't instantiate*/
	private ValidationUtil() {}

	/**
	 * Checks the given object for {@code null}. Throws a generic {@link IllegalArgumentException} if it is {@code null},
	 * otherwise returns the object.
	 *
	 * @param o
	 * 		the object to test
	 * @param <T>
	 * 		type safety parameter
	 * @return the object if it is not {@code null}
	 * @throws IllegalArgumentException
	 * 		if the object is {@code null}
	 * @see #requireNonNull(Object, String)
	 */
	public static<T> T requireNonNull(T o) {
		return requireNonNull(o, null);
	}

	/**
	 * Checks the given object for {@code null}. Throws a customized {@link IllegalArgumentException} if it is {@code null},
	 * otherwise returns the object.
	 *
	 * @param o
	 * 		the object to test
	 * @param name
	 * 		the name of the argument; optional
	 * @param <T>
	 * 		type safety parameter
	 * @return the object if it is not {@code null}
	 * @throws IllegalArgumentException
	 * 		if the object is {@code null}
	 */
	public static<T> T requireNonNull(T o, String name) {
		if (o == null) {
			name = StringUtils.trimToNull(name);
			throw new IllegalArgumentException(illegalArgumentMessage("Missing value", name));
		}
		return o;
	}

	/**
	 * Checks the given string for {@code null} and emptiness. Throws a generic {@link IllegalArgumentException}
	 * if it is {@code null} or empty, otherwise returns the string.
	 *
	 * @param string
	 * 		the string to test
	 * @return the string if it is not {@code null} or empty
	 * @throws IllegalArgumentException
	 * 		if the string is {@code null} or empty
	 * @see #requireNonEmptyString(String, String)
	 */
	public static String requireNonEmptyString(String string) {
		return requireNonEmptyString(string, null);
	}

	/**
	 * Checks the given string for {@code null} and emptiness. Throws a customized {@link IllegalArgumentException}
	 * if it is {@code null} or empty, otherwise returns the string.
	 *
	 * @param string
	 * 		the string to test
	 * @param name
	 * 		the name of the argument; optional
	 * @return the string if it is not {@code null} or empty
	 * @throws IllegalArgumentException
	 * 		if the string is {@code null} or empty
	 * @see #requireNonNull(Object, String)
	 */
	public static String requireNonEmptyString(String string, String name) {
		return requireNonNull(StringUtils.trimToNull(string), name);
	}

	/**
	 * Checks the given string for the dot-character. Throws a customized {@link IllegalArgumentException} if it
	 * contains a dot, otherwise returns the string.
	 *
	 * @param string
	 * 		the string to test
	 * @return the string if it does not contain a {@code .}
	 * @throws IllegalArgumentException
	 * 		if the string contains a dot
	 */
	public static String requireNoDot(String string) {
		return requireNoDot(string, null);
	}

	/**
	 * Checks the given string for the dot-character. Throws a customized {@link IllegalArgumentException} if it
	 * contains a dot, otherwise returns the string.
	 *
	 * @param string
	 * 		the string to test
	 * @param name
	 * 		the name of the argument; optional
	 * @return the string if it does not contain a {@code .}
	 * @throws IllegalArgumentException
	 * 		if the string contains a dot
	 */
	public static String requireNoDot(String string, String name) {
		if (string == null) {
			return null;
		}
		if (string.contains(DOT_AS_STRING)) {
			throw new IllegalArgumentException(illegalArgumentMessage("Strings containing dots", name));
		}
		return string;
	}

	/**
	 * Checks the given {@link List} for {@code null} and returns a copy of the list with all {@code null} elements removed.
	 *
	 * @param list
	 * 		the list to test
	 * @return a copy of the list with only non-{@code null} elements
	 * @see #stripToEmptyList(List, Predicate)
	 */
	public static <T> List<T> stripToEmptyList(List<T> list) {
		return stripToEmptyList(list, null);
	}

	/**
	 * Checks the given {@link List} for {@code null} and returns a copy of the list with all {@code null}/empty elements removed.
	 *
	 * @param list
	 * 		the list to test
	 * @param elementNotEmpty
	 * 		predicate to find non-empty elements, can be {@code null} and does not need to be {@code null} sensitive
	 * @return a copy of the list with only non-{@code null} elements
	 */
	public static <T> List<T> stripToEmptyList(List<T> list, Predicate<T> elementNotEmpty) {
		Predicate<T> acceptable;
		if (elementNotEmpty == null) {
			acceptable = Objects::nonNull;
		} else {
			acceptable = o -> Objects.nonNull(o) && elementNotEmpty.test(o);
		}
		if (list == null) {
			return new ArrayList<>();
		}
		return list.stream().filter(acceptable).collect(Collectors.toList());
	}

	/**
	 * Checks the given {@link List list(s)} for duplicates. Uniqueness will be measured by the given {@link Comparator}.
	 * Throws an {@link IllegalArgumentException} if a duplicate was found; stops at the first found duplicate.
	 * If no duplicates were found, returns the list.
	 *
	 * @param list
	 * 		the list to check for duplicates; must not be {@code null}
	 * @param measure
	 * 		the comparator to check uniqueness; optional, if not defined, the natural ordering is used
	 * @param otherLists
	 * 		additional lists to check duplication against; optional, can contain {@code null} entries
	 * @return the first list if no duplicates were found
	 * @throws IllegalArgumentException
	 * 		if duplicates were found
	 */
	public static <T, U extends T> List<U> noDuplicatesAllowed(List<U> list, Comparator<? super T> measure, List<? extends T>... otherLists) {
		return noDuplicatesAllowed(list, measure, null, otherLists);
	}

	/**
	 * Checks the given {@link List list(s)} for duplicates. Uniqueness will be measured by the given {@link Comparator}.
	 * Throws an {@link IllegalArgumentException} if a duplicate was found; stops at the first found duplicate.
	 * If no duplicates were found, returns the list.
	 *
	 * @param list
	 * 		the list to check for duplicates; must not be {@code null}
	 * @param measure
	 * 		the comparator to check uniqueness; optional, if not defined, the natural ordering is used
	 * @param name
	 * 		the name of the argument; optional
	 * @param otherLists
	 * 		additional lists to check duplication against; optional, can contain {@code null} entries
	 * @return the first list if no duplicates were found
	 * @throws IllegalArgumentException
	 * 		if duplicates were found
	 */
	@SafeVarargs
	public static <T, U extends T> List<U> noDuplicatesAllowed(List<U> list, Comparator<? super T> measure, String name, List<? extends T>... otherLists) {
		requireNonNull(list, name);
		if (list.isEmpty()) {
			return list;
		}
		Set<T> uniqueSet = new TreeSet<>(measure);
		name = StringUtils.trimToNull(name);
		String errorMessage = illegalArgumentMessage("Duplicates in list", name);
		for (T item : list) {
			if (!uniqueSet.add(item)) {
				throw new IllegalArgumentException(errorMessage + ": " + item);
			}
		}
		if (otherLists == null) {
			return list;
		}
		for (List<? extends T> otherList : otherLists) {
			if (otherList == null) {
				continue;
			}
			for (T item : otherList) {
				if (!uniqueSet.add(item)) {
					throw new IllegalArgumentException(errorMessage + ": " + item);
				}
			}
		}
		return list;
	}

	/**
	 * Checks the given {@link List} for {@code null}, only containing {@code null} and emptiness.
	 * Throws a generic {@link IllegalArgumentException} if it is {@code null}, contains only {@code null} or is empty,
	 * otherwise returns a copy of the list with all {@code null} elements removed.
	 *
	 * @param list
	 * 		the list to test
	 * @return a copy of the list with only non-{@code null} elements
	 * @throws IllegalArgumentException
	 * 		if the list is {@code null}, contains only {@code null} or is empty
	 * @see #requireNonEmptyList(List, Predicate, String)
	 */
	public static<T> List<T> requireNonEmptyList(List<T> list) {
		return requireNonEmptyList(list, null, null);
	}

	/**
	 * Checks the given {@link List} for {@code null}, only containing {@code null} and emptiness.
	 * Throws a customized {@link IllegalArgumentException} if it is {@code null}, contains only {@code null} or is empty,
	 * otherwise returns a copy of the list with all {@code null} elements removed.
	 *
	 * @param list
	 * 		the list to test
	 * @param name
	 * 		the name of the argument; optional
	 * @return a copy of the list with only non-{@code null} elements
	 * @throws IllegalArgumentException
	 * 		if the list is {@code null}, contains only {@code null} or is empty
	 * @see #requireNonEmptyList(List, Predicate, String)
	 */
	public static<T> List<T> requireNonEmptyList(List<T> list, String name) {
		return requireNonEmptyList(list, null, name);
	}

	/**
	 * Checks the given {@link List} for {@code null}, only containing {@code null} or empty elements and emptiness.
	 * Throws a customized {@link IllegalArgumentException} if it is {@code null}, contains only {@code null} or
	 * empty elements,or is empty, otherwise returns a copy of the list with all {@code null}/empty elements removed.
	 *
	 * @param list
	 * 		the list to test
	 * @param elementNotEmpty
	 * 		predicate to find non-empty elements, ca be {@code null}
	 * @return a copy of the list with only non-{@code null} elements
	 * @throws IllegalArgumentException
	 * 		if the list is {@code null}, contains only {@code null} or empty elements or is empty
	 * @see #requireNonEmptyList(List, Predicate, String)
	 */
	public static<T> List<T> requireNonEmptyList(List<T> list, Predicate<T> elementNotEmpty) {
		return requireNonEmptyList(list, elementNotEmpty, null);
	}

	/**
	 * Checks the given {@link List} for {@code null}, only containing {@code null} or empty elements and emptiness.
	 * Throws a customized {@link IllegalArgumentException} if it is {@code null}, contains only {@code null} or
	 * empty elements,or is empty, otherwise returns a copy of the list with all {@code null}/empty elements removed.
	 *
	 * @param list
	 * 		the list to test
	 * @param elementNotEmpty
	 * 		predicate to find non-empty elements, ca be {@code null}
	 * @param name
	 * 		the name of the argument; optional
	 * @return a copy of the list with only non-{@code null} elements
	 * @throws IllegalArgumentException
	 * 		if the list is {@code null}, contains only {@code null} or empty elements or is empty
	 */
	public static<T> List<T> requireNonEmptyList(List<T> list, Predicate<T> elementNotEmpty, String name) {
		requireNonNull(list, name);
		list = stripToEmptyList(list, elementNotEmpty);
		if (list.isEmpty()) {
			name = StringUtils.trimToNull(name);
			throw new IllegalArgumentException(illegalArgumentMessage("Empty, null-filled or missing list", name));
		}
		return list;
	}

	/**
	 * Same as {@link #dependencySortNoLoops(Function, Collection)}, but instead of throwing an {@link IllegalArgumentException}
	 * if a circular dependency is found, an empty list is returned.
	 */
	public static <K> List<K> dependencySortEmptyListForLoops(Function<K, ? extends Collection<K>> dependencyExtractor, Collection<K> toSort) {
		try {
			return dependencySortNoLoops(dependencyExtractor, toSort);
		} catch (IllegalArgumentException e) {
			return Collections.emptyList();
		}
	}

	/**
	 * Creates a copy of the given collection, whose sort order is specified by a topological sort over the dependencies
	 * generated by the given extractor.
	 * <br>
	 * First, the initial dependencies are extracted with the given function and then all transitive dependencies will
	 * be calculated. If a loop is detected, an {@link IllegalArgumentException} is thrown.
	 * <br>
	 * Second, all elements from the original collection are sorted by the dependencies, using insertion sort.
	 * The returned List is a {@link LinkedList}, and changing the list will not necessarily keep the order.
	 *
	 * @param dependencyExtractor
	 * 		the function to extract the initial dependencies from the original map
	 * @param toSort
	 * 		the collection to be sorted
	 * @param <K>
	 * 		the element type
	 * @return the sorted list
	 * @throws IllegalArgumentException
	 * 		if a circular dependency is found
	 * @see #getFullDependencies(Function, Collection)
	 * @see #sortListByDependency(Collection, Map)
	 */
	public static <K> List<K> dependencySortNoLoops(Function<K, ? extends Collection<K>> dependencyExtractor, Collection<K> toSort) {
		Map<K, Collection<K>> fullDependencies = getFullDependencies(dependencyExtractor, toSort);
		return sortListByDependency(toSort, fullDependencies);
	}

	/**
	 * Same as {@link #dependencySortNoLoops(BiFunction, Map)}, but instead of throwing an {@link IllegalArgumentException}
	 * if a circular dependency is found, an empty map is returned.
	 */
	public static <K, V> Map<K, V> dependencySortEmptyMapForLoops(BiFunction<K, V, ? extends Collection<K>> dependencyExtractor, Map<K, V> toSort) {
		try {
			return dependencySortNoLoops(dependencyExtractor, toSort);
		} catch (IllegalArgumentException e) {
			return Collections.emptyMap();
		}
	}

	/**
	 * Creates a copy of the given map, whose sort order is specified by a topological sort over the dependencies
	 * generated by the given extractor.
	 * <br>
	 * First, the initial dependencies are extracted with the given function and then all transitive dependencies will
	 * be calculated. If a loop is detected, an {@link IllegalArgumentException} is thrown.
	 * <br>
	 * Second, all keys from the original map are sorted by the dependencies, using insertion sort.
	 * <br>
	 * Lastly, a copy of the original map is created, honoring this new sorting. The sorting is <strong>NOT</strong> permanent,
	 * the returned map is a {@link LinkedHashMap}.
	 *
	 * @param dependencyExtractor
	 * 		the function to extract the initial dependencies from the original map
	 * @param toSort
	 * 		the map to be sorted
	 * @param <K>
	 * 		the key type
	 * @param <V>
	 * 		the value type
	 * @return the sorted map
	 * @throws IllegalArgumentException
	 * 		if a circular dependency is found
	 * @see #dependencySortNoLoops(Function, Collection)
	 */
	public static <K, V> Map<K, V> dependencySortNoLoops(BiFunction<K, V, ? extends Collection<K>> dependencyExtractor, Map<K, V> toSort) {
		List<K> sortedKeys = dependencySortNoLoops(k -> dependencyExtractor.apply(k, toSort.get(k)), toSort.keySet());
		return sortedKeys.stream().collect(Collectors.toMap(k -> k, toSort::get, (a, b) -> b, LinkedHashMap::new));
	}

	/**
	 * Calculates a new name based on the already known names. Will append a number in parenthesis if it is a duplicate.
	 * If the original name already ends with a number in parenthesis this method will increment the number.
	 *
	 * @param knownNames
	 * 		A collection that contains the already used names. This method will never generate a name that is already
	 * 		inside of this collection. Please note: The newly generated name will not be added to the collection.
	 * @param name
	 * 		The original name.
	 * @param returnBaseName
	 * 		If {@code true} and the original name ends with a number in parenthesis: This method will check whether the
	 * 		base name (original name without the number in parenthesis) is unique. If it is unique, then it will return the
	 * 		base name.
	 * @since 9.3
	 */
	public static String getNewName(Collection<String> knownNames, String name, boolean returnBaseName) {
		if (!knownNames.contains(name)) {
			return name;
		}
		String baseName = name;
		int index = baseName.lastIndexOf(" (");
		int i = 2;
		if (index >= 0 && baseName.endsWith(")")) {
			String suffix = baseName.substring(index + 2, baseName.length() - 1);
			try {
				i = Integer.parseInt(suffix) + 1;
				baseName = baseName.substring(0, index);
				if (returnBaseName && !knownNames.contains(baseName)) {
					return baseName;
				}
			} catch (NumberFormatException e) {
				// not a number; ignore, go with 2
			}
		}
		String newName;
		do {
			newName = baseName + " (" + i++ + ')';
		} while (knownNames.contains(newName));
		return newName;
	}

	/**
	 * Calls {@link ValidationUtil#getNewName(Collection, String, boolean)} with {@code returnBaseName = true}
	 *
	 * @since 9.3
	 */
	public static String getNewName(Collection<String> knownNames, String name) {
		return ValidationUtil.getNewName(knownNames, name, true);
	}

	/**
	 * Checks if the given {@link ConfigurationParameter} is set correctly.
	 *
	 * @param parameter
	 * 		the parameter, never {@code null}
	 * @return {@code true} if the parameter is injected or if it has a non-null and non-empty value; {@code false}
	 * otherwise
	 */
	public static boolean isValueSet(ConfigurationParameter parameter) {
		if (parameter == null) {
			throw new IllegalArgumentException("parameter must not be null!");
		}

		return parameter.isInjected() || parameter.getValue() != null && !parameter.getValue().trim().isEmpty();
	}


	/**
	 * Merges all provided values into a single array. Will result in an empty array if only null values or
	 * no values are provided.
	 *
	 * @param arraySupplier
	 * 		the array supplier
	 * @param values
	 * 		the list of values to merge
	 * @return the merged list of elements, possibly an empty array, never {@code null}
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] merge(IntFunction<T[]> arraySupplier, T[]... values) {
		return (values == null ? arraySupplier.apply(0) :
				Arrays.stream(values).filter(a -> a != null && a.length > 0)
						.flatMap(Arrays::stream).distinct().toArray(arraySupplier));
	}

	/** Create a map of full transitive dependencies. The initial dependencies are created using the given extractor. */
	private static <K> Map<K, Collection<K>> getFullDependencies(Function<K, ? extends Collection<K>> dependencyExtractor, Collection<K> extractable) {
		Map<K, Collection<K>> dependencies = extractable.stream().collect(Collectors.toMap(k -> k, dependencyExtractor, (a, b) -> b));
		addFullDependencies(dependencies);
		return dependencies;
	}

	/**
	 * Completes the given dependency map with all transitive dependencies. Will throw an {@link IllegalArgumentException}
	 * if circular dependencies are found.
	 */
	private static <K> void addFullDependencies(Map<K, ? extends Collection<K>> dependencies) {
		if (dependencies.isEmpty()) {
			return;
		}
		boolean change;
		do {
			change = false;
			dependencies.entrySet().stream().filter(e -> e.getValue().contains(e.getKey())).findAny().ifPresent(k -> {
				throw new IllegalArgumentException(illegalArgumentMessage("Circular dependencies", k.toString()));
			});
			for (Collection<K> deps : dependencies.values()) {
				if (deps.addAll(deps.stream().map(dependencies::get).filter(Objects::nonNull).flatMap(Collection::stream)
						.filter(k -> !deps.contains(k)).collect(Collectors.toSet()))) {
					change = true;
				}
			}
		} while (change);
	}

	/** Sorts the specified collection using the given dependency map and insertion sort. */
	private static <K> List<K> sortListByDependency(Collection<K> keys, Map<K, Collection<K>> fullDependencies) {
		if (keys.isEmpty()) {
			return new LinkedList<>();
		}
		Comparator<K> dependency = getDependencyComparator(fullDependencies);
		List<K> sortedKeys = new LinkedList<>();
		for (K key : keys) {
			if (sortedKeys.isEmpty()) {
				sortedKeys.add(key);
				continue;
			}
			int i = 0;
			for (; i < sortedKeys.size(); i++) {
				if (dependency.compare(key, sortedKeys.get(i)) < 0) {
					break;
				}
			}
			sortedKeys.add(i, key);
		}
		return sortedKeys;
	}

	/** Creates a {@link Comparator} that relies on the given dependency map */
	@SuppressWarnings("unchecked")
	private static <K> Comparator<K> getDependencyComparator(Map<K, Collection<K>> fullDependencies) {
		return (a, b) -> {
				Collection<K> aDependencies = fullDependencies.getOrDefault(a, Collections.emptySet());
				if (aDependencies.contains(b)) {
					return 1;
				}
				Collection<K> bDependencies = fullDependencies.getOrDefault(b, Collections.emptySet());
				if (bDependencies.contains(a)) {
					return -1;
				}
				if (aDependencies.size() != bDependencies.size()) {
					return aDependencies.size() - bDependencies.size();
				}
				if (a.getClass() == b.getClass() && a instanceof Comparable && b instanceof Comparable) {
					return ((Comparable<K>) a).compareTo(b);
				}
				return 0;
			};
	}

	private static String illegalArgumentMessage(String type, String name) {
		return type + (name != null ? " for \"" + name + "\"" : "") + " not allowed";
	}
}
