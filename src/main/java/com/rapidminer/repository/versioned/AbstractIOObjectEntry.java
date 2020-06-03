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
package com.rapidminer.repository.versioned;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.exceptions.DataRetrievalException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;


/**
 * The IOObject Entry super class for the versioned repository. Supports reading and writing {@link IOObject}s using
 * the {@link #getData()} and {@link #setData(Object)} methods.
 *
 * @author Andreas Timm, Gisa Meier
 * @since 9.7
 */
public abstract class AbstractIOObjectEntry<T extends IOObject> extends BasicDataEntry<T> implements IOObjectEntry {

	protected Class<? extends IOObject> dataClass;

	/**
	 * Create a new instance using {@link com.rapidminer.repository.Repository#createIOObjectEntry(String, IOObject,
	 * Operator, ProgressListener)}
	 *
	 * @param name
	 * 		full filename of the file without a path: "foo.bar"
	 * @param parent
	 *        {@link BasicFolder} is required
	 * @param dataType
	 * 		class of the datatype this Entry contains
	 */
	protected AbstractIOObjectEntry(String name, BasicFolder parent, Class<T> dataType) {
		super(name, parent, dataType);
	}

	@Override
	public IOObject retrieveData(ProgressListener l) throws RepositoryException {
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(10);
		}
		try {
			return getData();
		} catch (DataRetrievalException e) {
			throw new RepositoryException("Cannot load data from '" + getName() + "': " + e, e);
		}
	}

	/** Same as {@link #retrieveDataSummary()} */
	@Override
	public MetaData retrieveMetaData() throws RepositoryException {
		return (MetaData) retrieveDataSummary();
	}

	/** Checks wheter the given {@link DataSummary} is {@link MetaData} */
	@Override
	protected boolean checkDataSummary(DataSummary dataSummary) {
		return dataSummary instanceof MetaData;
	}

	@Override
	public Class<? extends IOObject> getObjectClass() {
		return dataClass;
	}

	@Override
	public void storeData(IOObject data, Operator callingOperator, ProgressListener l) throws RepositoryException {
		if (data == null) {
			throw new RepositoryException("IOObject to store must not be null!");
		}
		dataClass = data.getClass();
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(10);
		}

		try {
			setIOObjectData(data);
		} catch (RepositoryFileException | RepositoryImmutableException e) {
			throw new RepositoryException(e);
		}
		if (l != null) {
			l.complete();
		}
	}

	/**
	 * Calls the {@link #setData(Object)} method together with an instance check.
	 *
	 * @param data
	 * 		the data as an {@link IOObject}
	 */
	protected abstract void setIOObjectData(IOObject data) throws RepositoryFileException,
			RepositoryImmutableException, RepositoryException;

}
