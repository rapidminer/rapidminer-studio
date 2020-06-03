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

import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.GeneralFolder;
import com.rapidminer.versioning.repository.RepositoryFile;


/**
 * The basic entry belongs to a folder and repository and requires a type.
 *
 * @param <T> type of the content of this entry
 * @author Andreas Timm
 * @since 9.7
 */
public abstract class BasicEntry<T> extends RepositoryFile<T> implements Entry {

	/** Get repository access */
	private FilesystemRepositoryAdapter repository;


	/**
	 * Create a new basic entry with
	 *
	 * @param name     of this entry
	 * @param parent   folder of this entry
	 * @param dataType to be contained in this entry
	 */
	protected BasicEntry(String name, BasicFolder parent, Class<T> dataType) {
		super(name, parent.getFsFolder(), dataType);
		this.repository = parent.getRepositoryAdapter();
	}

	@Override
	public Folder getContainingFolder() {
		GeneralFolder parent = getParent();
		// if root, we need to return repository here to adhere to legacy repository contract
		if (parent.getParent() == null) {
			return getRepositoryAdapter();
		} else {
			return FilesystemRepositoryAdapter.toBasicFolder(getParent());
		}
	}

	/**
	 * Get the repository of this entry, which is a {@link FilesystemRepositoryAdapter}
	 *
	 * @return the repository in which this entry lives
	 */
	public FilesystemRepositoryAdapter getRepositoryAdapter() {
		return repository;
	}

	/**
	 * Retrieves the {@link DataSummary} for this {@link Entry}. Goes through various checks and might throw a
	 * {@link RepositoryException} if the data summary
	 * <ul>
	 *     <li>was {@code null}</li>
	 *     <li>is not acceptable ({@link #checkDataSummary(DataSummary)})</li>
	 *     <li>is a {@link FaultyDataSummary}, representing a repository exception</li>
	 *     <li>an unacceptable data summary, using {@link DataSummary#getSummary()} as the exception message</li>
	 * </ul>
	 *
	 * @return the data summary associated with this entry, never {@code null}
	 * @throws RepositoryException
	 * 		if the data summary retrieved does not fit the above criteria
	 */
	public DataSummary retrieveDataSummary() throws RepositoryException {
		DataSummary dataSummary = getRepositoryAdapter().getGeneralRepository().getDataSummary(this);
		if (dataSummary == null) {
			throw new RepositoryException("Could not read data summary, returned null");
		}
		if (checkDataSummary(dataSummary)) {
			return dataSummary;
		}
		if (dataSummary instanceof FaultyDataSummary) {
			throw (FaultyDataSummary) dataSummary;
		}
		throw new RepositoryException("Could not read proper data summary: " + dataSummary.getSummary());
	}

	/**
	 * Checks whether the given {@link DataSummary} is acceptable for this {@link Entry}.
	 *
	 * @param dataSummary
	 * 		the data summary to check; should not be {@code null}
	 * @return if the data summary is acceptable
	 */
	protected abstract boolean checkDataSummary(DataSummary dataSummary);
}

