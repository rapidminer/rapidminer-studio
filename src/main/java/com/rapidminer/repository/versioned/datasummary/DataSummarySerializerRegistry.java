/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.versioned.datasummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.rapidminer.repository.AbstractFileSuffixRegistry;
import com.rapidminer.tools.DominatingClassFinder;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.versioning.repository.DataSummary;

/**
 * Registry for {@link DataSummarySerializer}. Serializers can be queried by suffix ({@link #getCallback(String)} or by
 * {@link DataSummary} class ({@link #getCallbacks(Class)}, in which case a list of serializers will be returned.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public class DataSummarySerializerRegistry extends AbstractFileSuffixRegistry<DataSummarySerializer> {

	private static DataSummarySerializerRegistry instance;

	private final Map<Class<? extends DataSummary>, List<DataSummarySerializer>> serializerByClass = new ConcurrentHashMap<>();

	static {
		ExampleSetDataSummarySerializer.INSTANCE.register();
		ConnectionDataSummarySerializer.INSTANCE.register();
	}

	/**
	 * Get the registry instance.
	 *
	 * @return the instance, never {@code null}
	 */
	public static synchronized DataSummarySerializerRegistry getInstance() {
		if (instance == null) {
			instance = new DataSummarySerializerRegistry();
		}
		return instance;
	}

	/**
	 * @param suffix {@inheritDoc}; must be the same as {@link DataSummarySerializer#getSuffix()}
	 */
	@Override
	public boolean registerCallback(String suffix, DataSummarySerializer callback) {
		ValidationUtil.requireNonNull(callback, "callback");
		suffix = prepareSuffix(suffix);
		if (!callback.getSuffix().equals(suffix)) {
			throw new IllegalArgumentException("suffix of callback must be the same as registered");
		}
		boolean registered = super.registerCallback(suffix, callback);
		if (registered) {
			serializerByClass.computeIfAbsent(callback.getSummaryClass(),
					c -> new CopyOnWriteArrayList<>()).add(callback);
		}
		return registered;
	}

	/**
	 * @param suffix {@inheritDoc}; must be the same as {@link DataSummarySerializer#getSuffix()}
	 */
	@Override
	public void unregisterCallback(String suffix, DataSummarySerializer callback) {
		ValidationUtil.requireNonNull(callback, "callback");
		suffix = prepareSuffix(suffix);
		if (!callback.getSuffix().equals(suffix)) {
			throw new IllegalArgumentException("suffix of callback must be the same as registered");
		}
		super.unregisterCallback(suffix, callback);
		serializerByClass.getOrDefault(callback.getSummaryClass(), new ArrayList<>()).remove(callback);
	}

	/**
	 * Finds a list of suitable {@link DataSummarySerializer} that can handle the given {@link DataSummary} class.
	 * Uses {@link DominatingClassFinder} to go through the class hierarchy if necessary
	 *
	 * @param dsClass
	 * 		the class to serialize
	 * @return a list of serializer candidates; might be empty
	 */
	public List<DataSummarySerializer> getCallbacks(Class<? extends DataSummary> dsClass) {
		List<DataSummarySerializer> serializers = new ArrayList<>(serializerByClass.getOrDefault(dsClass, Collections.emptyList()));
		while(serializers.isEmpty()) {
			Class<? extends DataSummary> dominatingClass = new DominatingClassFinder<DataSummary>()
					.findNextDominatingClass(dsClass, serializerByClass.keySet());
			if (dominatingClass == null || dominatingClass == dsClass) {
				// prevent endless loop
				return Collections.emptyList();
			}
			dsClass = dominatingClass;
			serializers = new ArrayList<>(serializerByClass.getOrDefault(dsClass, Collections.emptyList()));
			Class<? extends DataSummary> testClass = dsClass;
			serializers.removeIf(ser -> !ser.canHandle(testClass));
		}
		return serializers;
	}

	/**
	 * Finds a list of suitable {@link DataSummarySerializer} that can handle the given {@link DataSummary} instance.
	 * Uses {@link DominatingClassFinder} to go through the class hierarchy if necessary
	 *
	 * @param ds
	 * 		the data summary to serialize
	 * @return a list of serializer candidates; might be empty
	 */
	public List<DataSummarySerializer> getCallbacks(DataSummary ds) {
		Class<? extends DataSummary> dsClass = ds.getClass();
		List<DataSummarySerializer> serializers = new ArrayList<>(serializerByClass.getOrDefault(dsClass, Collections.emptyList()));
		while(serializers.isEmpty()) {
			Class<? extends DataSummary> dominatingClass = new DominatingClassFinder<DataSummary>()
					.findNextDominatingClass(dsClass, serializerByClass.keySet());
			if (dominatingClass == null || dominatingClass == dsClass) {
				// prevent endless loop
				return Collections.emptyList();
			}
			dsClass = dominatingClass;
			serializers = new ArrayList<>(serializerByClass.getOrDefault(dsClass, Collections.emptyList()));
			serializers.removeIf(ser -> !ser.canHandle(ds));
		}
		return serializers;
	}


	/** @return a list of all registered suffixes */
	public List<String> getRegisteredSuffixes() {
		return serializerByClass.values().stream().flatMap(List::stream)
				.map(DataSummarySerializer::getSuffix).collect(Collectors.toList());
	}
}
