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

import java.io.IOException;
import java.nio.file.Path;

import com.rapidminer.repository.PersistentContentMapperStore.ContentDeserializer;
import com.rapidminer.repository.PersistentContentMapperStore.ContentSerializer;
import com.rapidminer.versioning.repository.DataSummary;

/**
 * Base interface for handling {@link DataSummary} serialization. Incorporates serialization from
 * {@link com.rapidminer.repository.PersistentContentMapperStore PersistentContentMapperStore}
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public interface DataSummarySerializer extends ContentSerializer<DataSummary>, ContentDeserializer<DataSummary> {

	/** @return the suffix this serializer is associated with, without leading dot */
	String getSuffix();

	/**
	 * Returns the highest possible {@link DataSummary} class that this can handle.
	 * Can be expanded on more in {@link #canHandle(DataSummary)} and {@link #canHandle(Class)}.
	 */
	Class<? extends DataSummary> getSummaryClass();

	/** Registers this serializer to the {@link DataSummarySerializerRegistry} */
	default void register() {
		DataSummarySerializerRegistry.getInstance().registerCallback(getSuffix(), this);
	}

	/** Whether or not this serializer can handle the given {@link DataSummary} */
	default boolean canHandle(DataSummary ds) {
		return ds != null && canHandle(ds.getClass());
	}

	/** Whether or not this serializer can handle the given {@link DataSummary} class */
	default boolean canHandle(Class<? extends DataSummary> dsClass) {
		return dsClass != null && getSummaryClass().isAssignableFrom(dsClass);
	}

	@Override
	default void serialize(Path path, DataSummary dataSummary) throws IOException {
		// noop
	}

	@Override
	default DataSummary deserialize(Path path) throws IOException {
		return null;
	}
}
