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
package com.rapidminer.tools.container;


import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.rapidminer.tools.LogService;


/**
 * This {@link Map} implementation uses a map of {@link Stack Stacks} as a backend.
 * It acts like a stack grouped by keys, so {@link #get(Object)} will correspond to {@link Stack#peek()},
 * {@link #put(Object, Object)} to {@link Stack#push(Object)} and {@link #remove(Object)} to {@link Stack#pop()}.
 * <p>
 * <strong>Note:</strong> {@link java.util.SortedMap SortedMaps} that use a custom comparator are not suitable to be wrapped at this point.
 * <p>
 * <strong>Note:</strong> This class is not thread safe by default.
 *
 * @author Jan Czogalla
 * @since 8.2
 */
public class StackingMap<K, V> implements Map<K, V> {

	private static final Stack DEFAULT_STACK = new Stack();

	private final Map<K, Stack<V>> stackedMap;

	/**
	 * Default constructor. Will use a {@link HashMap} as the backing map.
	 */
	public StackingMap() {
		stackedMap = new HashMap<>();
	}

	/**
	 * Constructs a stacking map from any given map, using that maps class as the
	 * backing map class. Will push all entries from the given map.
	 *
	 * @param map
	 * 		the map to be wrapped
	 */
	public StackingMap(Map<K, V> map) {
		this(map.getClass());
		putAll(map);
	}

	/**
	 * Clone constructor. Will create a deep copy of the backing map.
	 */
	public StackingMap(StackingMap<K, V> stackingMap) {
		this(stackingMap.stackedMap.getClass());
		stackingMap.stackedMap.forEach((k, s) -> this.stackedMap.put(k, (Stack<V>) s.clone()));
	}

	/**
	 * Constructs a stacking map using the given map class for the backing map.
	 * Will use {@link HashMap} on {@code null} argument or if an error occurs.
	 */
	@SuppressWarnings("unchecked")
	public StackingMap(Class<? extends Map> mapClass) {
		if (mapClass == null) {
			stackedMap = new HashMap<>();
			return;
		}
		Map<K, Stack<V>> map = null;
		try {
			map = (Map<K, Stack<V>>) mapClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.container.StackingMap.failed_map_initialization", mapClass.getName());
		}
		stackedMap = map != null ? map : new HashMap<>();
	}

	@Override
	public int size() {
		return stackedMap.size();
	}

	@Override
	public boolean isEmpty() {
		return stackedMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return stackedMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return value != null && values().contains(value);
	}

	@Override
	public V get(Object key) {
		return peek(key);
	}

	/**
	 * Looks at the object at the top of the stack specified by the given key without removing it
	 * from the stack. Will return {@code null} if no stack is associated or it is empty.
	 *
	 * @return the object at the top of the specified stack (the last item
	 * of the <tt>Vector</tt> object) or {@code null}
	 * @throws NullPointerException
	 * 		if the underlying map does not support {@code null} keys
	 */
	public V peek(Object key) {
		Stack<V> stack = stackedMap.getOrDefault(key, DEFAULT_STACK);
		return stack.empty() ? null : stack.peek();
	}

	/**
	 * Associates the specified value with the specified key in this map. If the map previously contained
	 * a mapping for the key, the specified value is stacked atop the old value.
	 *
	 * @see #peek(Object)
	 * @see #push(Object, Object)
	 */
	@Override
	public V put(K key, V value) {
		V prev = peek(key);
		push(key, value);
		return prev;
	}

	/**
	 * Pushes a non-{@code null} item onto the top of the stack associated with the given key.
	 * Will ignore {@code null} items.
	 *
	 * @param key
	 * 		the key whose associated stack should be pushed to
	 * @param item
	 * 		the item to be pushed onto the stack
	 * @return the {@code item} argument
	 * @throws NullPointerException
	 * 		if the underlying map does not support {@code null} keys
	 */
	public V push(K key, V item) {
		if (item == null) {
			return null;
		}
		Stack<V> stack = stackedMap.computeIfAbsent(key, k -> new Stack());
		return stack.push(item);
	}

	/**
	 * Removes all associated values for this key and returns the last {@link #push(Object, Object) pushed} object.
	 *
	 * @return the last pushed obejct or {@code null} if none are present.
	 */
	@Override
	public V remove(Object key) {
		V oldValue = peek(key);
		stackedMap.remove(key);
		return oldValue;
	}

	/**
	 * Removes the object at the top of the stack specified by the given key and
	 * returns that object as the value of this function. Will return {@code null} if no
	 * stack is associated or it is empty.
	 *
	 * @param key
	 * 		the key whose associated stack should be popped from
	 * @return The object at the top of the specified stack (the last item
	 * of the <tt>Vector</tt> object) or {@code null}
	 * @throws NullPointerException
	 * 		if the underlying map does not support {@code null} keys
	 */
	public V pop(Object key) {
		Stack<V> stack = stackedMap.getOrDefault(key, DEFAULT_STACK);
		V value = stack.empty() ? null : stack.pop();
		if (stack != DEFAULT_STACK && stack.empty()) {
			stackedMap.remove(key);
		}
		return value;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		map.forEach(this::push);
	}

	@Override
	public void clear() {
		stackedMap.clear();
	}

	@Override
	public Set<K> keySet() {
		return stackedMap.keySet();
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 * The collection is not backed by the map, so changes to the map are
	 * not reflected in the collection, and vice-versa. The order of the collection
	 * will be the same as in the backing map.
	 */
	@Override
	public Collection<V> values() {
		return stackedMap.values().stream().map(Stack::peek).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 * The set is not backed by the map, so changes to the map are
	 * not reflected in the set, and vice-versa. The order of the set will
	 * be the same as in the backing map.
	 */
	@Override
	public Set<Entry<K, V>> entrySet() {
		return stackedMap.entrySet().stream().map(e -> new SimpleEntry<K, V>(e.getKey(), e.getValue().peek())).
				collect(Collectors.toCollection(LinkedHashSet::new));
	}

}
